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
import java.time.LocalDateTime

@Service
class OfficialVisitUpdateService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val contactsService: ContactsService,
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

    val updatedVisit = officialVisitRepository.saveAndFlush(changedOVEntity)

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
    )

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

    val existingVisitors = ove.officialVisitors()

    val visitorsToAdd = request.officialVisitors.filter {
      it.officialVisitorId !in existingVisitors.map { existing -> existing.officialVisitorId }
    }

    val visitorsToUpdate = request.officialVisitors.filter {
      it.officialVisitorId in existingVisitors.map { existing -> existing.officialVisitorId }
    }.associateBy { it.officialVisitorId }

    val visitorsToRemove = existingVisitors.filter {
      it.officialVisitorId !in request.officialVisitors.map { req -> req.officialVisitorId }
    }

    val matchingContacts = request.getMatchingContactDetails(ove.prisonerNumber).filterNotNull()

    val visitorsAdded = addNewVisitors(ove, visitorsToAdd, matchingContacts, user)
    val visitorsDeleted = deleteExistingVisitors(ove, visitorsToRemove)
    val visitorsUpdated = updateExistingVisitors(visitorsToUpdate, matchingContacts, user)

    return OfficialVisitUpdateVisitorsResponse(
      officialVisitId = officialVisitId,
      prisonCode = prisonCode,
      prisonerNumber = ove.prisonerNumber,
      visitorsAdded = visitorsAdded,
      visitorsDeleted = visitorsDeleted,
      visitorsUpdated = visitorsUpdated,
    )
  }

  private fun deleteExistingVisitors(ove: OfficialVisitEntity, visitorsToRemove: List<OfficialVisitorEntity>) = visitorsToRemove.map { visitor ->
    ove.removeVisitor(visitor)
    OfficialVisitorUpdated(officialVisitorId = visitor.officialVisitorId, contactId = visitor.contactId!!)
  }

  private fun addNewVisitors(
    visit: OfficialVisitEntity,
    visitorsToAdd: List<OfficialVisitor>,
    matchingContacts: List<PrisonerContact>,
    user: User,
  ): List<OfficialVisitorUpdated> {
    val response: MutableList<OfficialVisitorUpdated> = emptyList<OfficialVisitorUpdated>().toMutableList()

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

      response.add(
        OfficialVisitorUpdated(officialVisitorId = savedVisitor.officialVisitorId, contactId = savedVisitor.contactId!!),
      )
    }
    return response
  }

  private fun updateExistingVisitors(
    visitorsToUpdate: Map<Long, OfficialVisitor>,
    matchingContacts: List<PrisonerContact>,
    user: User,
  ): List<OfficialVisitorUpdated> {
    val response: MutableList<OfficialVisitorUpdated> = emptyList<OfficialVisitorUpdated>().toMutableList()

    visitorsToUpdate.forEach { (officialVisitorId, visitor) ->
      val matchingPerson = matchingContacts.singleOrNull { it.contactId == visitor.contactId }

      val visitorEntity = officialVisitorRepository.findById(officialVisitorId).orElseThrow {
        EntityNotFoundException("Cannot find visitor with id $officialVisitorId to update")
      }

      if (visitorChanged(visitorEntity, visitor, matchingPerson)) {
        visitorEntity.apply {
          relationshipTypeCode = if (matchingPerson?.relationshipTypeCode == "S") RelationshipType.SOCIAL else RelationshipType.OFFICIAL
          relationshipCode = matchingPerson?.relationshipToPrisonerCode
          prisonerContactId = visitorEntity?.prisonerContactId ?: matchingPerson?.prisonerContactId
          firstName = visitorEntity?.firstName ?: matchingPerson?.firstName ?: "Unknown"
          lastName = visitorEntity?.lastName ?: matchingPerson?.lastName ?: "Unknown"
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

        response.add(
          OfficialVisitorUpdated(officialVisitorId = savedVisitor.officialVisitorId, contactId = savedVisitor.contactId!!),
        )
      }
    }

    return response
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
    var hasChanged = false
    if (old.firstName != person?.firstName) {
      hasChanged = true
    }
    if (old.lastName != person?.lastName) {
      hasChanged = true
    }
    if (old.leadVisitor != new.leadVisitor) {
      hasChanged = true
    }
    if (old.assistedVisit != new.assistedVisit) {
      hasChanged = true
    }
    if (old.relationshipCode != new.relationshipCode) {
      hasChanged = true
    }
    if (old.visitorNotes != new.assistedNotes) {
      hasChanged = true
    }
    if (old.visitorEquipment?.description != new.visitorEquipment?.description) {
      hasChanged = true
    }
    return hasChanged
  }
}
