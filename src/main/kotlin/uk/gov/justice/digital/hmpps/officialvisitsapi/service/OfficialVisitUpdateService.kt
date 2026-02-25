package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.xml.bind.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateCommentRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitVisitorUpdate
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitorUpdated
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class OfficialVisitUpdateService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val contactsService: ContactsService,
) {

  fun updateVisitTypeAndSlot(officialVisitId: Long, prisonCode: String, request: OfficialVisitUpdateSlotRequest, user: User) {
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
    officialVisitRepository.saveAndFlush(changedOVEntity)
  }

  fun updateComments(officialVisitId: Long, prisonCode: String, request: OfficialVisitUpdateCommentRequest, user: User) {
    val ove = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(officialVisitId, prisonCode)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId and prison code $prisonCode not found")

    officialVisitRepository.saveAndFlush(
      ove.apply {
        staffNotes = request.staffNotes!!
        prisonerNotes = request.prisonerNotes!!
        updatedBy = user.username
        updatedTime = LocalDateTime.now()
      },
    )
  }

  fun updateVisitors(
    officialVisitId: Long,
    prisonCode: String,
    request: OfficialVisitUpdateVisitorsRequest,
    user: User,
  ): OfficialVisitVisitorUpdate {
    val ove = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(officialVisitId, prisonCode)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId and prison code $prisonCode not found")
    val matchingVisitors = request.getVisitorDetails(ove.prisonerNumber)
    val existingVisitors = ove.officialVisitors()
    val updateVisitors =
      request.officialVisitors.filter { it.officialVisitorId in existingVisitors.map { it.officialVisitorId } }.associateBy { it.officialVisitorId }
    val newVisitors = request.officialVisitors.filter { it.officialVisitorId == 0L }

    // Add new visitors
    ove.addOrUpdateVisitors(newVisitors, matchingVisitors, user)
    // updated visitors
    ove.addOrUpdateVisitors(request.officialVisitors.filter { it.officialVisitorId != 0L }, matchingVisitors, user)

    val removedVisitorList = existingVisitors.filter {
      it.officialVisitorId !in updateVisitors
    }
      .map {
        ove.removeVisitor(it)
        OfficialVisitorUpdated(
          officialVisitorId = it.officialVisitorId,
          contactId = it.contactId!!,
        )
      }
    val updatedOV = officialVisitRepository.saveAndFlush(ove)
    val visitors = updatedOV.officialVisitors()

    // add visitors
    val addedVisitorsList = visitors.filter {
      it.officialVisitorId !in existingVisitors.map { it.officialVisitorId }
    }
      .map {
        OfficialVisitorUpdated(
          officialVisitorId = it.officialVisitorId,
          contactId = it.contactId!!,
        )
      }

    // update visitors
    val updatedVisitorsList = visitors.filter {
      it.officialVisitorId in existingVisitors.map { it.officialVisitorId }
    }
      .map {
        OfficialVisitorUpdated(
          officialVisitorId = it.officialVisitorId,
          contactId = it.contactId!!,
        )
      }

    return OfficialVisitVisitorUpdate(
      officialVisitId = officialVisitId,
      prisonCode = prisonCode,
      prisonerNumber = updatedOV.prisonerNumber,
      visitorsAdded = addedVisitorsList,
      visitorsDeleted = removedVisitorList,
      visitorsUpdated = updatedVisitorsList,
    )
  }

  private fun OfficialVisitEntity.addOrUpdateVisitors(
    officialVisitors: List<OfficialVisitor>,
    matchingVisitors: List<ApprovedContact>,
    user: User,
  ) = apply {
    officialVisitors.forEach { ov ->
      val matchingVisitor =
        matchingVisitors.single { mv -> mv.contactId == ov.contactId && mv.prisonerContactId == ov.prisonerContactId }
      addVisitor(
        visitorTypeCode = ov.visitorTypeCode!!,
        relationshipTypeCode = if (matchingVisitor.relationshipTypeCode == "S") RelationshipType.SOCIAL else RelationshipType.OFFICIAL,
        relationshipCode = ov.relationshipCode!!,
        contactId = ov.contactId,
        prisonerContactId = ov.prisonerContactId,
        firstName = matchingVisitor.firstName,
        lastName = matchingVisitor.lastName,
        leadVisitor = ov.leadVisitor ?: false,
        assistedVisit = ov.assistedVisit ?: false,
        assistedNotes = ov.assistedNotes,
        createdBy = user,
      )
    }
  }

  private fun OfficialVisitUpdateVisitorsRequest.getVisitorDetails(prisonerNumber: String) = run {
    val requestedVisitors = officialVisitors.filter { it.visitorTypeCode == VisitorType.CONTACT }.toSet()
    val approvedVisitors = contactsService.getApprovedContacts(prisonerNumber)

    requestedVisitors.map { requestedVisitor ->
      val matchingVisitor =
        approvedVisitors.singleOrNull { approvedVisitor -> approvedVisitor.contactId == requestedVisitor.contactId && approvedVisitor.prisonerContactId == requestedVisitor.prisonerContactId }

      requireNotNull(matchingVisitor) {
        "Visitor with contact ID ${requestedVisitor.contactId} and prisoner contact ID ${requestedVisitor.prisonerContactId} is not approved for visiting prisoner number $prisonerNumber."
      }
      matchingVisitor
    }
  }
}
