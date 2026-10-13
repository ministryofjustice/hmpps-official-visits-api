package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.xml.bind.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitorEquipmentEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateCommentRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitUpdateCommentsResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitUpdateSlotResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitUpdateVisitorsResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitorUpdated
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditEventType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitChangeEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitorChangedEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.VisitMetricInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.VisitorMetricInfo
import java.time.LocalDateTime

@Service
class OfficialVisitUpdateService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val contactsService: ContactsService,
  private val metricsService: MetricsService,
  private val locationsService: LocationsService,
  private val auditingService: AuditingService,
) {
  /**
   * Update the visit type and its date, time and location
   */
  @Transactional
  fun updateVisitTypeAndSlot(
    officialVisitId: Long,
    prisonCode: String,
    request: OfficialVisitUpdateSlotRequest,
    user: User,
  ): OfficialVisitUpdateSlotResponse {
    val ove = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(officialVisitId, prisonCode)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId and prison code $prisonCode not found")

    val newPrisonVisitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId)
      .orElseThrow { throw ValidationException("Prison visit slot with id ${request.prisonVisitSlotId} not found.") }

    val auditChangeEvent = auditVisitChangeEvent {
      officialVisitId(ove.officialVisitId)
      summaryText(AuditEventType.VISIT_UPDATED)
      eventSource("DPS")
      user(user)
      prisonCode(ove.prisonCode)
      prisonerNumber(ove.prisonerNumber)
      changes {
        change("visit_date", ove.visitDate, request.visitDate)
        change("start_time", ove.startTime, request.startTime)
        change("end_time", ove.endTime, request.endTime)
        if (ove.dpsLocationId != request.dpsLocationId) {
          val oldLocation = locationsService.getLocationById(ove.dpsLocationId)
          val newLocation = locationsService.getLocationById(request.dpsLocationId)

          change("location", oldLocation?.localName, newLocation?.localName)
        }
        change("visit_type", ove.visitTypeCode, request.visitTypeCode)
        change("visit_slot", ove.prisonVisitSlot.prisonVisitSlotId, newPrisonVisitSlot.prisonVisitSlotId)
      }
    }

    val changedOVEntity = ove.apply {
      prisonVisitSlot = newPrisonVisitSlot
      visitDate = request.visitDate
      startTime = request.startTime
      endTime = request.endTime
      dpsLocationId = request.dpsLocationId
      visitTypeCode = request.visitTypeCode
      updatedBy = user.username
      updatedTime = LocalDateTime.now()
    }

    val updatedVisit = officialVisitRepository.saveAndFlush(changedOVEntity).also {
      metricsService.send(
        MetricsEvents.AMEND,
        VisitMetricInfo(
          username = user.username,
          officialVisitId = it.officialVisitId,
          prisonCode = it.prisonCode,
          prisonerNumber = it.prisonerNumber,
          numberOfVisitors = it.officialVisitors().size.toLong(),
          startTime = it.startTime,
        ),
      )
    }

    auditingService.recordAuditEvent(auditChangeEvent)

    return OfficialVisitUpdateSlotResponse(
      officialVisitId = updatedVisit.officialVisitId,
      prisonerNumber = updatedVisit.prisonerNumber,
    )
  }

  /**
   * Update only the staff and prisoner notes.
   */
  @Transactional
  fun updateComments(
    officialVisitId: Long,
    prisonCode: String,
    request: OfficialVisitUpdateCommentRequest,
    user: User,
  ): OfficialVisitUpdateCommentsResponse {
    val ove = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(officialVisitId, prisonCode)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId and prison code $prisonCode not found")

    val auditChangeEvent = auditVisitChangeEvent {
      officialVisitId(ove.officialVisitId)
      summaryText(AuditEventType.VISIT_UPDATED)
      eventSource("DPS")
      user(user)
      prisonCode(ove.prisonCode)
      prisonerNumber(ove.prisonerNumber)
      changes {
        change("prisoner_notes", ove.prisonerNotes, request.prisonerNotes)
        change("staff_notes", ove.staffNotes, request.staffNotes)
      }
    }

    val updatedVisit = officialVisitRepository.saveAndFlush(
      ove.apply {
        staffNotes = request.staffNotes
        prisonerNotes = request.prisonerNotes
        updatedBy = user.username
        updatedTime = LocalDateTime.now()
      },
    ).also {
      metricsService.send(
        MetricsEvents.AMEND,
        VisitMetricInfo(
          username = user.username,
          officialVisitId = it.officialVisitId,
          prisonCode = it.prisonCode,
          prisonerNumber = it.prisonerNumber,
          numberOfVisitors = it.officialVisitors().size.toLong(),
          startTime = it.startTime,
        ),
      )
    }

    auditingService.recordAuditEvent(auditChangeEvent)

    return OfficialVisitUpdateCommentsResponse(
      officialVisitId = updatedVisit.officialVisitId,
      prisonerNumber = updatedVisit.prisonerNumber,
    )
  }

  /**
   * Replace the visitors on a visit with the visitors in the request.
   *
   * - officialVisitorId == 0: a new visitor, added.
   * - officialVisitorId != 0: an existing visitor to keep, updated only if something changed.
   * - Existing visitors missing from the request are removed.
   *
   * Every requested visitor must match exactly one current prisoner contact. The whole request is validated and
   * resolved before anything is changed.
   */
  @Transactional
  fun updateVisitors(
    officialVisitId: Long,
    prisonCode: String,
    request: OfficialVisitUpdateVisitorsRequest,
    user: User,
  ): OfficialVisitUpdateVisitorsResponse {
    val ove = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(officialVisitId, prisonCode)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId and prison code $prisonCode not found")

    val existingVisitors = ove.officialVisitors().associateBy { it.officialVisitorId }

    // 1. Validate and resolve: work out exactly what will change before touching anything
    val (requestedNew, requestedExisting) = request.officialVisitors.partition { it.isNewVisitor() }
    val keptVisitorIds = requestedExisting.map { it.officialVisitorId }.toSet()

    require(keptVisitorIds.size == requestedExisting.size) {
      "Request contains duplicate visitors for official visit with id $officialVisitId"
    }
    require(existingVisitors.keys.containsAll(keptVisitorIds)) {
      "Request contains visitors which do not exist on official visit with id $officialVisitId"
    }

    // Only call out to the contacts API once the request is structurally valid
    val contacts = contactsService.getAllPrisonerContacts(prisonerNumber = ove.prisonerNumber, approved = null, currentTerm = true)

    val visitorsToAdd = requestedNew.map { visitor -> visitor to contacts.matchFor(visitor) }

    val visitorsToUpdate = requestedExisting
      .map { visitor -> Triple(existingVisitors.getValue(visitor.officialVisitorId), visitor, contacts.matchFor(visitor)) }
      .filter { (entity, visitor, contact) -> entity.hasChanges(visitor, contact) }

    // Remove any existing visitors which are not in the request
    val visitorsToRemove = existingVisitors.values.filterNot { it.officialVisitorId in keptVisitorIds }

    // 2. Apply
    visitorsToRemove.forEach(ove::removeVisitor)

    val updated = visitorsToUpdate.map { (entity, visitor, contact) ->
      officialVisitorRepository.saveAndFlush(entity.applyUpdate(visitor, contact, user))
    }

    val added = visitorsToAdd.map { (visitor, contact) ->
      officialVisitorRepository.saveAndFlush(newVisitorEntity(ove, visitor, contact, user))
    }

    // 3. Audit and metrics
    auditingService.recordAuditEvent(
      auditVisitorChangedEvent {
        officialVisitId(ove.officialVisitId)
        summaryText(AuditEventType.VISITOR_CHANGED)
        eventSource("DPS")
        user(user)
        prisonCode(ove.prisonCode)
        prisonerNumber(ove.prisonerNumber)
        changes {
          added.forEach { visitorAdded(it.auditName()) }
          updated.forEach { visitorUpdated(it.auditName()) }
          visitorsToRemove.forEach { visitorRemoved(it.auditName()) }
        }
      },
    )

    added.forEach { sendVisitorMetric(MetricsEvents.ADD_VISITOR, it, officialVisitId, prisonCode, user) }
    visitorsToRemove.forEach { sendVisitorMetric(MetricsEvents.REMOVE_VISITOR, it, officialVisitId, prisonCode, user) }

    return OfficialVisitUpdateVisitorsResponse(
      officialVisitId = officialVisitId,
      prisonCode = prisonCode,
      prisonerNumber = ove.prisonerNumber,
      visitorsAdded = added.map { it.toVisitorUpdated() },
      visitorsDeleted = visitorsToRemove.map { it.toVisitorUpdated() },
      visitorsUpdated = updated.map { it.toVisitorUpdated() },
    )
  }

  private fun newVisitorEntity(
    visit: OfficialVisitEntity,
    visitor: OfficialVisitor,
    contact: PrisonerContact,
    user: User,
  ) = OfficialVisitorEntity(
    officialVisit = visit,
    visitorTypeCode = VisitorType.CONTACT,
    relationshipTypeCode = contact.relationshipType(),
    relationshipCode = contact.relationshipToPrisonerCode,
    contactId = visitor.contactId,
    prisonerContactId = contact.prisonerContactId,
    firstName = contact.firstName,
    lastName = contact.lastName,
    leadVisitor = visitor.leadVisitor ?: false,
    assistedVisit = visitor.assistedVisit ?: false,
    visitorNotes = visitor.assistedNotes,
    createdBy = user.username,
    createdTime = LocalDateTime.now(),
  ).apply {
    visitor.equipmentDescription()?.let { visitorEquipment = equipment(it, user) }
  }

  /**
   * True if applying the request would change the visitor. Compares against the values applyUpdate will
   * actually write, so the two always agree.
   */
  private fun OfficialVisitorEntity.hasChanges(visitor: OfficialVisitor, contact: PrisonerContact) = prisonerContactId == null ||
    firstName == null ||
    lastName == null ||
    relationshipTypeCode != contact.relationshipType() ||
    relationshipCode != contact.relationshipToPrisonerCode ||
    leadVisitor != (visitor.leadVisitor ?: false) ||
    assistedVisit != (visitor.assistedVisit ?: false) ||
    visitorNotes != visitor.assistedNotes ||
    visitorEquipment?.description != visitor.equipmentDescription()

  private fun OfficialVisitorEntity.applyUpdate(visitor: OfficialVisitor, contact: PrisonerContact, user: User) = apply {
    relationshipTypeCode = contact.relationshipType()
    relationshipCode = contact.relationshipToPrisonerCode
    leadVisitor = visitor.leadVisitor ?: false
    assistedVisit = visitor.assistedVisit ?: false
    visitorNotes = visitor.assistedNotes
    val newEquipment = visitor.equipmentDescription()
    if (visitorEquipment?.description != newEquipment) {
      visitorEquipment = newEquipment?.let { equipment(it, user) }
    }
    // Backfill identity fields on older records; values captured at booking are kept
    prisonerContactId = prisonerContactId ?: contact.prisonerContactId
    firstName = firstName ?: contact.firstName
    lastName = lastName ?: contact.lastName
    updatedBy = user.username
    updatedTime = LocalDateTime.now()
  }

  private fun sendVisitorMetric(
    event: MetricsEvents,
    visitor: OfficialVisitorEntity,
    officialVisitId: Long,
    prisonCode: String,
    user: User,
  ) = metricsService.send(
    event,
    VisitorMetricInfo(
      source = Source.DPS,
      username = user.username,
      prisonCode = prisonCode,
      officialVisitId = officialVisitId,
      contactId = visitor.contactId!!,
      officialVisitorId = visitor.officialVisitorId,
    ),
  )

  /**
   * The request is invalid unless the visitor matches exactly one current prisoner contact. prisonerContactId is
   * nullable on older visitors, so fall back to the relationship code when it is absent.
   */
  private fun List<PrisonerContact>.matchFor(visitor: OfficialVisitor): PrisonerContact = singleOrNull {
    it.contactId == visitor.contactId &&
      if (visitor.prisonerContactId != null) {
        it.prisonerContactId == visitor.prisonerContactId
      } else {
        it.relationshipToPrisonerCode == visitor.relationshipCode
      }
  } ?: throw ValidationException("Invalid request: No matching prisoner contact found for contactId=${visitor.contactId}, prisonerContactId=${visitor.prisonerContactId}")

  /**
   * Return a human-readable name for the visitor, or "unknown" if the name is missing. Used in audit events.
   */
  private fun OfficialVisitorEntity.auditName(): String = listOfNotNull(firstName, lastName)
    .filter { it.isNotBlank() }
    .joinToString(" ") { it.toTitleCase() }
    .ifBlank { "unknown" }

  private fun OfficialVisitorEntity.equipment(description: String, user: User) = VisitorEquipmentEntity(
    officialVisitor = this,
    description = description,
    createdBy = user.username,
  )

  private fun OfficialVisitorEntity.toVisitorUpdated() = OfficialVisitorUpdated(officialVisitorId = officialVisitorId, contactId = contactId!!)

  private fun OfficialVisitor.isNewVisitor() = officialVisitorId == 0L

  private fun OfficialVisitor.equipmentDescription() = visitorEquipment?.description?.takeIf { it.isNotBlank() }

  private fun PrisonerContact.relationshipType() = if (relationshipTypeCode == "S") RelationshipType.SOCIAL else RelationshipType.OFFICIAL

  private fun String.toTitleCase() = lowercase().replaceFirstChar { it.uppercase() }
}
