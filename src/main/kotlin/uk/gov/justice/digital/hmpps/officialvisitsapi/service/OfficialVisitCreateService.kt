package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository

@Service
class OfficialVisitCreateService(
  private val prisonerValidator: PrisonerValidator,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val officialVisitRepository: OfficialVisitRepository,
) {
  fun create(request: CreateOfficialVisitRequest, user: User): CreateOfficialVisitResponse = run {
    val prisonVisitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId)
      .orElseThrow { throw ValidationException("Prison visit slot with id ${request.prisonVisitSlotId} not found.") }

    request
      .checkPrisonerDetails()
      .checkContactDetails()
      .checkStillAvailable(prisonVisitSlot)

    officialVisitRepository.saveAndFlush(
      OfficialVisitEntity(
        prisonVisitSlot = prisonVisitSlot,
        prisonCode = request.prisonCode!!,
        prisonerNumber = request.prisonerNumber!!,
        visitDate = request.visitDate!!,
        startTime = request.startTime!!,
        endTime = request.endTime!!,
        dpsLocationId = request.dpsLocationId!!,
        visitStatusCode = VisitStatusType.SCHEDULED,
        visitTypeCode = request.visitTypeCode!!,
        staffNotes = request.staffNotes,
        prisonerNotes = request.prisonerNotes,
        createdBy = user.username,
      ).apply {
        request.officialVisitors.forEach {
          addVisitor(
            visitorTypeCode = it.visitorTypeCode!!,
            firstName = it.firstName,
            lastName = it.lastName,
            contactId = it.contactId,
            prisonerContactId = it.prisonerContactId,
            relationshipTypeCode = it.relationshipTypeCode!!,
            relationshipCode = it.relationshipCode!!,
            leadVisitor = it.leadVisitor ?: false,
            assistedVisit = it.assistedVisit ?: false,
            visitorNotes = it.visitorNotes,
            createdBy = user,
          )
        }
      },
    ).let {
      CreateOfficialVisitResponse(it.officialVisitId)
    }
  }

  private fun CreateOfficialVisitRequest.checkStillAvailable(prisonVisitSlot: PrisonVisitSlotEntity) = also {
    // TODO check prison visit slot is still available prior to persisting.
  }

  private fun CreateOfficialVisitRequest.checkPrisonerDetails() = also {
    prisonerValidator.validatePrisonerAtPrison(prisonerNumber!!, prisonCode!!)
  }

  private fun CreateOfficialVisitRequest.checkContactDetails() = also {
    require(officialVisitors.isNotEmpty()) { "At least one official visitor must be supplied." }

    // TODO check contact is active and is a valid contact for the prisoner
    // TODO check the contacts relationship to the prison in line with what has supplied in the request
  }
}
