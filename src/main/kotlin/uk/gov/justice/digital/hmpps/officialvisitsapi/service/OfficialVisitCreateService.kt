package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.time.LocalDateTime.now

@Service
class OfficialVisitCreateService(
  private val prisonerValidator: PrisonerValidator,
  private val availableSlotService: AvailableSlotService,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
) {
  @Transactional
  fun create(request: CreateOfficialVisitRequest, user: User): CreateOfficialVisitResponse = run {
    val prisonVisitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId)
      .orElseThrow { throw ValidationException("Prison visit slot with id ${request.prisonVisitSlotId} not found.") }

    val prisonerDetails = request.getPrisonersDetails()

    request
      .checkVisitDateAndTimes()
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
        offenderBookId = prisonerDetails.bookingId?.toLong(),
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
    ).also {
      prisonerVisitedRepository.saveAndFlush(
        PrisonerVisitedEntity(
          officialVisit = it,
          prisonerNumber = it.prisonerNumber,
          attendanceCode = null,
          createdBy = user.username,
        ),
      )
    }.let {
      CreateOfficialVisitResponse(it.officialVisitId)
    }
  }

  private fun CreateOfficialVisitRequest.checkVisitDateAndTimes() = also {
    require(visitDate!!.atTime(startTime) > now()) { "Official visit cannot be scheduled in the past" }
    require(startTime!! < endTime) { "Official visit start time must be before end time" }
  }

  private fun CreateOfficialVisitRequest.checkStillAvailable(prisonVisitSlot: PrisonVisitSlotEntity) = also {
    val slots = availableSlotService.getAvailableSlotsForPrison(
      prisonCode = prisonCode!!,
      fromDate = visitDate!!,
      toDate = visitDate,
      videoOnly = visitTypeCode!! == VisitType.VIDEO,
    )

    require(
      slots.any { it.visitSlotId == prisonVisitSlot.prisonVisitSlotId && it.startTime == startTime && it.dpsLocationId == dpsLocationId },
    ) {
      "Prison visit slot ${prisonVisitSlot.prisonVisitSlotId} is no longer available for the requested date and time."
    }
  }

  private fun CreateOfficialVisitRequest.getPrisonersDetails() = run {
    prisonerValidator.validatePrisonerAtPrison(prisonerNumber!!, prisonCode!!)
  }

  private fun CreateOfficialVisitRequest.checkContactDetails() = also {
    require(officialVisitors.isNotEmpty()) { "At least one official visitor must be supplied." }

    // TODO check contact is active and is a valid contact for the prisoner
    // TODO check the contacts relationship to the prison in line with what has supplied in the request
  }
}
