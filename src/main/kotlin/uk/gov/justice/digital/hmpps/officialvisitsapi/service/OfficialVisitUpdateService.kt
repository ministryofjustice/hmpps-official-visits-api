package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.xml.bind.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitorEquipmentEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.ContactMatchingException
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
   * Update the visitors on the visit, adding, removing and updating as required.
   *
   * In the request from the UI the following values are meaningful:
   *  - officialVisitId == 0 - means its a new visitor that has been added
   *  - officialVisitorId > 0 - means a pre-existing visitor to update if changed.
   *
   * If any existing visitors which are missing from the request are to be removed.
   *
   * Every visitor requested must be in a relationship with the prisoner, however,
   * that relationship may have changed (i.e. a different prisonerContactId or
   * relationship code), and in some cases the contact may have changed their name,
   * so these are checked and are updated on the visitor.
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

    // Retrieve the visitors on the currently saved version of this visit
    val existingVisitors = ove.officialVisitors().associateBy { it.officialVisitorId }

    // Identify and new visitors in the request - they have a 0L in the officialVisitorId field
    val newVisitors = request.officialVisitors.filter { it.isNewVisitor() }

    // Identify the updated visitors - those in the request that have a non-zero officialVisitorId
    val updatedVisitors = request.officialVisitors.filterNot { it.isNewVisitor() }.associateBy { it.officialVisitorId }

    // Identify the removed visitors - those in the existing visitors list but not in update list
    val removedVisitors = existingVisitors.values.filterNot { updatedVisitors.containsKey(it.officialVisitorId) }

    // Error if the update list contains any visitor IDs who are not in the existing list of visitors
    require(updatedVisitors.keys.all { existingVisitors.containsKey(it) }) {
      "Request contains updates for visitors which do not exist on visit ID $officialVisitId"
    }

    // Error if the requested list of visitors contains multiple people with the same contactId (same person appearing twice)
    require(request.officialVisitors.distinctBy { it.contactId }.size == request.officialVisitors.size) {
      "Request contains multiple visitors who are the same person (contact ID). This should be unique per visit. Visit ID $officialVisitId"
    }

    // Get all the prisoner's contacts, whether approved to visit or not, for the prisoner's current term in prison
    val allPrisonerContacts = contactsService.getAllPrisonerContacts(prisonerNumber = ove.prisonerNumber, approved = null, currentTerm = true)

    // Build the response and save the changes made to the visitors
    val response = OfficialVisitUpdateVisitorsResponse(
      officialVisitId = officialVisitId,
      prisonCode = prisonCode,
      prisonerNumber = ove.prisonerNumber,
      visitorsAdded = addNewVisitors(ove, newVisitors, allPrisonerContacts, user),
      visitorsDeleted = removeVisitors(ove, removedVisitors),
      visitorsUpdated = updateExistingVisitors(ove, updatedVisitors, allPrisonerContacts, user),
    )

    // Record the audit events for each visitor added, changed or removed
    auditingService.recordAuditEvent(
      auditVisitorChangedEvent {
        officialVisitId(ove.officialVisitId)
        summaryText(AuditEventType.VISITOR_CHANGED)
        eventSource("DPS")
        user(user)
        prisonCode(ove.prisonCode)
        prisonerNumber(ove.prisonerNumber)
        changes {
          newVisitors.forEach { visitor -> visitorAdded(allPrisonerContacts.matchFor(officialVisitId, visitor).fullName()) }
          updatedVisitors.forEach { (_, visitor) -> visitorUpdated(allPrisonerContacts.matchFor(officialVisitId, visitor).fullName()) }
          removedVisitors.forEach { visitorEntity -> visitorRemoved(allPrisonerContacts.matchForEntity(officialVisitId, visitorEntity).fullName()) }
        }
      },
    )

    // Record a metric for each of the new and removed visitors
    recordVisitorMetrics(officialVisitId, prisonCode, MetricsEvents.ADD_VISITOR, response.visitorsAdded, user)
    recordVisitorMetrics(officialVisitId, prisonCode, MetricsEvents.REMOVE_VISITOR, response.visitorsDeleted, user)

    return response
  }

  private fun recordVisitorMetrics(
    officialVisitId: Long,
    prisonCode: String,
    metricsEvent: MetricsEvents,
    visitors: List<OfficialVisitorUpdated>,
    user: User,
  ) = visitors.forEach { visitor ->
    metricsService.send(
      eventType = metricsEvent,
      info = VisitorMetricInfo(
        source = Source.DPS,
        username = user.username,
        prisonCode = prisonCode,
        officialVisitId = officialVisitId,
        contactId = visitor.contactId,
        officialVisitorId = visitor.officialVisitorId,
      ),
    )
  }

  private fun PrisonerContact.fullName() = "${firstName.lowercase().replaceFirstChar { it.uppercase() }} ${lastName.lowercase().replaceFirstChar { it.uppercase() }}"

  private fun OfficialVisitor.isNewVisitor() = officialVisitorId == 0L

  /**
   * Removes visitors from the visit entity provided, and returns a list of
   * OfficialVisitorUpdated objects, one for each removed
   */
  private fun removeVisitors(ove: OfficialVisitEntity, visitorsToRemove: List<OfficialVisitorEntity>) = visitorsToRemove.map { visitor ->
    ove.removeVisitor(visitor)
    OfficialVisitorUpdated(officialVisitorId = visitor.officialVisitorId, contactId = visitor.contactId!!)
  }

  /**
   * Adds visitors to the visit entity provided, and returns a list of OfficialVisitorUpdated, one for
   * each visitor added. A failure to match visitor with a prisoner's contacts will roll back the transaction.
   */
  private fun addNewVisitors(
    visit: OfficialVisitEntity,
    visitorsToAdd: Collection<OfficialVisitor>,
    matchingContacts: List<PrisonerContact>,
    user: User,
  ) = buildList {
    visitorsToAdd.forEach { visitor ->
      // If no match this will throw a ContactMatchingException, a subclass of RuntimeException
      val matchingPerson = matchingContacts.matchFor(visit.officialVisitId, visitor)

      val savedVisitor = officialVisitorRepository.saveAndFlush(
        OfficialVisitorEntity(
          officialVisit = visit,
          visitorTypeCode = VisitorType.CONTACT,
          relationshipTypeCode = if (matchingPerson.relationshipTypeCode == "S") RelationshipType.SOCIAL else RelationshipType.OFFICIAL,
          relationshipCode = matchingPerson.relationshipToPrisonerCode,
          contactId = visitor.contactId,
          prisonerContactId = matchingPerson.prisonerContactId,
          firstName = matchingPerson.firstName,
          lastName = matchingPerson.lastName,
          leadVisitor = visitor.leadVisitor ?: false,
          assistedVisit = visitor.assistedVisit ?: false,
          visitorNotes = visitor.assistedNotes,
          createdBy = user.username,
          createdTime = LocalDateTime.now(),
        ).apply {
          // Save the visitor equipment, if present in the request
          visitor.visitorEquipment?.description?.let { description ->
            visitorEquipment = VisitorEquipmentEntity(
              officialVisitor = this,
              description = description,
              createdBy = user.username,
            )
          }
        },
      )

      add(
        OfficialVisitorUpdated(
          officialVisitorId = savedVisitor.officialVisitorId,
          contactId = savedVisitor.contactId!!,
        ),
      )
    }
  }

  /**
   * Updates the visitors on the visit entity provided, and returns a list
   * of OfficialVisitorUpdated, one for each updated.
   */
  private fun updateExistingVisitors(
    visit: OfficialVisitEntity,
    visitorsToUpdate: Map<Long, OfficialVisitor>,
    matchingContacts: List<PrisonerContact>,
    user: User,
  ) = buildList {
    visitorsToUpdate.forEach { (officialVisitorId, visitor) ->
      // If no match this will throw a ContactMatchingException, a subclass of RuntimeException
      val matchingPerson = matchingContacts.matchFor(visit.officialVisitId, visitor)

      val visitorEntity = officialVisitorRepository.findById(officialVisitorId).orElseThrow {
        EntityNotFoundException("Cannot find visitor with id $officialVisitorId to update")
      }

      if (visitorChanged(visitorEntity, visitor, matchingPerson)) {
        visitorEntity.apply {
          relationshipTypeCode = if (matchingPerson.relationshipTypeCode == "S") RelationshipType.SOCIAL else RelationshipType.OFFICIAL
          relationshipCode = matchingPerson.relationshipToPrisonerCode
          prisonerContactId = matchingPerson.prisonerContactId
          firstName = matchingPerson.firstName
          lastName = matchingPerson.lastName
          leadVisitor = visitor.leadVisitor ?: false
          assistedVisit = visitor.assistedVisit ?: false
          visitorNotes = visitor.assistedNotes
          visitorEquipment = visitor.visitorEquipment?.description
            ?.takeIf { it.isNotBlank() }
            ?.let {
              VisitorEquipmentEntity(
                officialVisitor = this,
                description = it,
                createdBy = user.username,
              )
            }
          updatedBy = user.username
          updatedTime = LocalDateTime.now()
        }

        val savedVisitor = officialVisitorRepository.saveAndFlush(visitorEntity)

        add(
          OfficialVisitorUpdated(
            officialVisitorId = savedVisitor.officialVisitorId,
            contactId = savedVisitor.contactId!!,
          ),
        )
      }
    }
  }

  /**
   * Determines whether a visitor has changed in any material sense between current and requested update.
   * This could be an updated relationship - same or different prisonerContactId, or relationship type.
   */
  private fun visitorChanged(old: OfficialVisitorEntity, new: OfficialVisitor, person: PrisonerContact?): Boolean {
    if (old.firstName != person?.firstName) {
      return true
    }
    if (old.lastName != person?.lastName) {
      return true
    }
    if (old.leadVisitor != new.leadVisitor) {
      return true
    }
    if (old.prisonerContactId != person?.prisonerContactId) {
      return true
    }
    if (old.assistedVisit != new.assistedVisit) {
      return true
    }
    if (old.relationshipTypeCode?.name != person?.relationshipTypeCode) {
      return true
    }
    if (old.relationshipCode != new.relationshipCode) {
      return true
    }
    if (old.visitorNotes != new.assistedNotes) {
      return true
    }
    if (old.visitorEquipment?.description != new.visitorEquipment?.description) {
      return true
    }
    return false
  }

  /**
   * Matches a visitor model requested with a person and relationship in the prisoner's list of contacts.
   * Matches on
   * - same contactId && prisonerContactId, OR
   * - same contactId && relationshipCode OR
   * - same contactId, provided there's only one of these
   */
  private fun List<PrisonerContact>.matchFor(officialVisitId: Long, visitor: OfficialVisitor): PrisonerContact = singleOrNull {
    it.contactId == visitor.contactId &&
      if (visitor.prisonerContactId != null) {
        it.prisonerContactId == visitor.prisonerContactId
      } else {
        it.relationshipToPrisonerCode == visitor.relationshipCode
      }
  } ?: throw ContactMatchingException(
    officialVisitId,
    visitor.officialVisitorId,
    visitor.contactId,
    "Invalid request: No matching prisoner contact found for officialVisitId=$officialVisitId, officialVisitorId=${visitor.officialVisitorId}, contactId=${visitor.contactId}, prisonerContactId=${visitor.prisonerContactId}",
  )

  /**
   * Matches a visitor entity with a person and relationship in the prisoner's list of contacts.
   * Matches on
   * - same contactId && prisonerContactId, OR
   * - same contactId && relationshipCode OR
   * - same contactId, provided there's only one of these
   */
  private fun List<PrisonerContact>.matchForEntity(officialVisitId: Long, visitorEntity: OfficialVisitorEntity): PrisonerContact = singleOrNull {
    it.contactId == visitorEntity.contactId &&
      if (visitorEntity.prisonerContactId != null) {
        it.prisonerContactId == visitorEntity.prisonerContactId
      } else {
        it.relationshipToPrisonerCode == visitorEntity.relationshipCode
      }
  } ?: throw ContactMatchingException(
    officialVisitId,
    visitorEntity.officialVisitorId,
    visitorEntity.contactId,
    "Invalid request: No matching prisoner contact found for officialVisitId=$officialVisitId, officialVisitorId=${visitorEntity.officialVisitorId}, contactId=${visitorEntity.contactId}, prisonerContactId=${visitorEntity.prisonerContactId}",
  )
}
