package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.xml.bind.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitorEquipmentEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateCommentRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitUpdateCommentsResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitUpdateSlotResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitUpdateVisitorsResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitorUpdated
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.time.LocalDateTime

@Service
@Transactional
class OfficialVisitUpdateService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val contactsService: ContactsService,
) {

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

  fun updateVisitors(
    officialVisitId: Long,
    prisonCode: String,
    request: OfficialVisitUpdateVisitorsRequest,
    user: User,
  ): OfficialVisitUpdateVisitorsResponse {
    val ove = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(officialVisitId, prisonCode)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId and prison code $prisonCode not found")

    val matchingVisitors = request.getVisitorDetails(ove.prisonerNumber)

    val existingVisitors = ove.officialVisitors()

    val newVisitors = request.officialVisitors.filter { it.officialVisitorId == 0L }

    val updatedVisitors = request.officialVisitors.filter {
      it.officialVisitorId in existingVisitors.map { existing -> existing.officialVisitorId }
    }.associateBy { it.officialVisitorId }

    ove.addVisitors(newVisitors, matchingVisitors, user)

    ove.updateVisitors(updatedVisitors, matchingVisitors, user)

    val removedVisitorList = existingVisitors.filter {
      it.officialVisitorId !in updatedVisitors
    }
      .map {
        ove.removeVisitor(it)
        OfficialVisitorUpdated(
          officialVisitorId = it.officialVisitorId,
          contactId = it.contactId!!,
        )
      }

    val updatedVisit = officialVisitRepository.saveAndFlush(ove)

    val visitors = updatedVisit.officialVisitors()

    val addedVisitorsList = visitors.filter {
      it.officialVisitorId !in existingVisitors.map { existing -> existing.officialVisitorId }
    }
      .map {
        OfficialVisitorUpdated(
          officialVisitorId = it.officialVisitorId,
          contactId = it.contactId!!,
        )
      }

    val updatedVisitorsList = visitors.filter {
      it.officialVisitorId in existingVisitors.map { existing -> existing.officialVisitorId }
    }
      .map {
        OfficialVisitorUpdated(
          officialVisitorId = it.officialVisitorId,
          contactId = it.contactId!!,
        )
      }

    return OfficialVisitUpdateVisitorsResponse(
      officialVisitId = officialVisitId,
      prisonCode = prisonCode,
      prisonerNumber = updatedVisit.prisonerNumber,
      visitorsAdded = addedVisitorsList,
      visitorsDeleted = removedVisitorList,
      visitorsUpdated = updatedVisitorsList,
    )
  }

  private fun OfficialVisitEntity.addVisitors(
    officialVisitors: List<OfficialVisitor>,
    matchingVisitors: List<ApprovedContact>,
    user: User,
  ) = apply {
    officialVisitors.forEach { ov ->
      val matchingVisitor = matchingVisitors.single { mv ->
        mv.contactId == ov.contactId && mv.prisonerContactId == ov.prisonerContactId
      }

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
      ).apply {
        ov.visitorEquipment?.description?.let { description ->
          visitorEquipment = VisitorEquipmentEntity(
            officialVisitor = this,
            description = description,
            createdBy = user.username,
          )
        }
      }
    }
  }

  private fun OfficialVisitEntity.updateVisitors(
    updateVisitors: Map<Long, OfficialVisitor>,
    matchingVisitors: List<ApprovedContact>,
    user: User,
  ) = run {
    officialVisitors().forEach { visitor ->
      updateVisitors[visitor.officialVisitorId]?.let { changedVisitorEntity ->
        run {
          val matchingVisitor = matchingVisitors.single { mv ->
            mv.contactId == changedVisitorEntity.contactId && mv.prisonerContactId == changedVisitorEntity.prisonerContactId
          }

          visitor.apply {
            visitorTypeCode = changedVisitorEntity.visitorTypeCode!!
            contactId = changedVisitorEntity.contactId
            prisonerContactId = changedVisitorEntity.prisonerContactId
            relationshipCode = changedVisitorEntity.relationshipCode
            leadVisitor = changedVisitorEntity.leadVisitor ?: false
            assistedVisit = changedVisitorEntity.assistedVisit ?: false
            visitorNotes = changedVisitorEntity.assistedNotes
            updatedBy = user.username
            updatedTime = LocalDateTime.now()
            firstName = matchingVisitor.firstName
            lastName = matchingVisitor.lastName
            relationshipTypeCode = if (matchingVisitor.relationshipTypeCode == "S") RelationshipType.SOCIAL else RelationshipType.OFFICIAL
            offenderVisitVisitorId = visitor.offenderVisitVisitorId
            attendanceCode = visitor.attendanceCode
            visitorEquipment = changedVisitorEntity.visitorEquipment?.description
              ?.takeIf { it.isNotBlank() }
              ?.let {
                VisitorEquipmentEntity(
                  officialVisitor = this,
                  description = it,
                  createdBy = user.username,
                )
              }
          }
        }
      }
    }
  }

  // TODO: Revisit this when relaxing the rules about whether contacts are active or approved
  private fun OfficialVisitUpdateVisitorsRequest.getVisitorDetails(prisonerNumber: String) = run {
    val requestedVisitors = officialVisitors.filter { it.visitorTypeCode == VisitorType.CONTACT }.toSet()
    val approvedVisitors = contactsService.getApprovedContacts(prisonerNumber)

    requestedVisitors.map { requestedVisitor ->
      val matchingVisitor = approvedVisitors.singleOrNull { approvedVisitor ->
        approvedVisitor.contactId == requestedVisitor.contactId && approvedVisitor.prisonerContactId == requestedVisitor.prisonerContactId
      }

      requireNotNull(matchingVisitor) {
        "Visitor with contact ID ${requestedVisitor.contactId} and prisoner contact ID ${requestedVisitor.prisonerContactId} is not approved for visiting prisoner number $prisonerNumber."
      }

      matchingVisitor
    }
  }
}
