package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.xml.bind.ValidationException
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OVActions
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OfficialVisitMetricTelemetryService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitMetricInfo
import java.time.LocalDateTime

@Service
class OfficialVisitUpdateService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val contactsService: ContactsService,
  val officialVisitMetricTelemetryService: OfficialVisitMetricTelemetryService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

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

    val newPrisonVisitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId!!)
      .orElseThrow { throw ValidationException("Prison visit slot with id ${request.prisonVisitSlotId} not found.") }

    val changedOVEntity = ove.apply {
      prisonVisitSlot = newPrisonVisitSlot
      visitDate = request.visitDate!!
      startTime = request.startTime!!
      endTime = request.endTime!!
      dpsLocationId = request.dpsLocationId!!
      visitTypeCode = request.visitTypeCode!!
      updatedBy = user.username
      updatedTime = LocalDateTime.now()
    }

    val updatedVisit = officialVisitRepository.saveAndFlush(changedOVEntity).also {
      officialVisitMetricTelemetryService.send(
        MetricsEvents.VISIT_UPDATED,
        action = OVActions.AMEND,
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

    val updatedVisit = officialVisitRepository.saveAndFlush(
      ove.apply {
        staffNotes = request.staffNotes
        prisonerNotes = request.prisonerNotes
        updatedBy = user.username
        updatedTime = LocalDateTime.now()
      },
    ).also {
      officialVisitMetricTelemetryService.send(
        MetricsEvents.VISIT_UPDATED,
        action = OVActions.COMPLETE,
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

    return OfficialVisitUpdateCommentsResponse(
      officialVisitId = updatedVisit.officialVisitId,
      prisonerNumber = updatedVisit.prisonerNumber,
    )
  }

  /**
   * Update the visitors on the visit, adding, removing and updating as required.
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

    val (newVisitors, updatedVisitors, removedVisitors) = run {
      val new = request.officialVisitors.filter { it.isNewVisitor() }.associateBy { it.officialVisitorId }
      val updates = request.officialVisitors.filterNot { it.isNewVisitor() }.associateBy { it.officialVisitorId }
      val removals = existingVisitors.values.filterNot { new.plus(updates).containsKey(it.officialVisitorId) }

      require(updates.keys.all { existingVisitors.containsKey(it) }) {
        "Request contains visitors which do not exist on official visit with id $officialVisitId"
      }

      Triple(new, updates, removals)
    }

    val matchingContacts = request.getMatchingContactDetails(ove.prisonerNumber).filterNotNull()

    return OfficialVisitUpdateVisitorsResponse(
      officialVisitId = officialVisitId,
      prisonCode = prisonCode,
      prisonerNumber = ove.prisonerNumber,
      visitorsAdded = addNewVisitors(ove, newVisitors.values, matchingContacts, user),
      visitorsDeleted = deleteExistingVisitors(ove, removedVisitors),
      visitorsUpdated = updateExistingVisitors(updatedVisitors, matchingContacts, user),
    )
  }

  private fun OfficialVisitor.isNewVisitor() = officialVisitorId == 0L

  private fun deleteExistingVisitors(ove: OfficialVisitEntity, visitorsToRemove: List<OfficialVisitorEntity>) = visitorsToRemove.map { visitor ->
    ove.removeVisitor(visitor)
    OfficialVisitorUpdated(officialVisitorId = visitor.officialVisitorId, contactId = visitor.contactId!!)
  }

  private fun addNewVisitors(
    visit: OfficialVisitEntity,
    visitorsToAdd: Collection<OfficialVisitor>,
    matchingContacts: List<PrisonerContact>,
    user: User,
  ) = buildList {
    visitorsToAdd.forEach { visitor ->
      val matchingPerson = matchingContacts.singleOrNull { it.contactId == visitor.contactId }

      val savedVisitor = officialVisitorRepository.saveAndFlush(
        OfficialVisitorEntity(
          officialVisit = visit,
          visitorTypeCode = VisitorType.CONTACT,
          relationshipTypeCode = if (matchingPerson?.relationshipTypeCode == "S") RelationshipType.SOCIAL else RelationshipType.OFFICIAL,
          relationshipCode = matchingPerson?.relationshipToPrisonerCode,
          contactId = visitor.contactId,
          prisonerContactId = matchingPerson?.prisonerContactId,
          firstName = matchingPerson?.firstName ?: "Unknown",
          lastName = matchingPerson?.lastName ?: "Unknown",
          leadVisitor = visitor.leadVisitor ?: false,
          assistedVisit = visitor.assistedVisit ?: false,
          visitorNotes = visitor.assistedNotes,
          createdBy = user.username,
          createdTime = LocalDateTime.now(),
        ).apply {
          visitor.visitorEquipment?.description?.let { description ->
            visitorEquipment = VisitorEquipmentEntity(
              officialVisitor = this,
              description = description,
              createdBy = user.username,
            )
          }
        },
      )

      add(OfficialVisitorUpdated(officialVisitorId = savedVisitor.officialVisitorId, contactId = savedVisitor.contactId!!))
    }
  }

  private fun updateExistingVisitors(
    visitorsToUpdate: Map<Long, OfficialVisitor>,
    matchingContacts: List<PrisonerContact>,
    user: User,
  ) = buildList {
    visitorsToUpdate.forEach { (officialVisitorId, visitor) ->
      val matchingPerson = matchingContacts.singleOrNull { it.contactId == visitor.contactId }

      val visitorEntity = officialVisitorRepository.findById(officialVisitorId).orElseThrow {
        EntityNotFoundException("Cannot find visitor with id $officialVisitorId to update")
      }

      if (visitorChanged(visitorEntity, visitor, matchingPerson)) {
        visitorEntity.apply {
          relationshipTypeCode =
            if (matchingPerson?.relationshipTypeCode == "S") RelationshipType.SOCIAL else RelationshipType.OFFICIAL
          relationshipCode = matchingPerson?.relationshipToPrisonerCode
          prisonerContactId = visitorEntity?.prisonerContactId ?: matchingPerson?.prisonerContactId
          firstName = visitorEntity?.firstName ?: matchingPerson?.firstName ?: "Unknown"
          lastName = visitorEntity?.lastName ?: matchingPerson?.lastName ?: "Unknown"
          leadVisitor = visitor.leadVisitor ?: false
          assistedVisit = visitor.assistedVisit ?: false
          visitorNotes = visitor.assistedNotes
          visitorEquipment = visitor.visitorEquipment?.description
            ?.takeIf { it.isNotBlank() }
            ?.let { VisitorEquipmentEntity(officialVisitor = this, description = it, createdBy = user.username) }
          updatedBy = user.username
          updatedTime = LocalDateTime.now()
        }

        val savedVisitor = officialVisitorRepository.saveAndFlush(visitorEntity)

        add(OfficialVisitorUpdated(officialVisitorId = savedVisitor.officialVisitorId, contactId = savedVisitor.contactId!!))
      }
    }
  }

  private fun OfficialVisitUpdateVisitorsRequest.getMatchingContactDetails(prisonerNumber: String) = run {
    val contacts = contactsService.getAllPrisonerContacts(prisonerNumber = prisonerNumber, approved = null, currentTerm = true)

    officialVisitors.map { requestedVisitor ->
      val matchingVisitor = contacts.singleOrNull { visitor -> visitor.contactId == requestedVisitor.contactId }
      if (matchingVisitor == null) {
        log.info("INFO only - Contact ID ${requestedVisitor.contactId} was not found in the current relationships for prisoner number $prisonerNumber.")
      }
      matchingVisitor
    }
  }

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
    if (old.assistedVisit != new.assistedVisit) {
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
}
