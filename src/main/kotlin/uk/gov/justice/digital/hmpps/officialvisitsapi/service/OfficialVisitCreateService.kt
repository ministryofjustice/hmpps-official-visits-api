package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitorEquipmentEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact
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
  private val contactsService: ContactsService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun create(prisonCode: String, request: CreateOfficialVisitRequest, user: User): CreateOfficialVisitResponse = run {
    val prisonVisitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId)
      .orElseThrow { throw ValidationException("Prison visit slot with id ${request.prisonVisitSlotId} not found.") }

    request.checkVisitDateAndTimes()

    val prisonerDetails = request.getPrisonersDetails(prisonCode)
    val matchingVisitors = request.getVisitorDetails()

    request.checkStillAvailable(prisonCode, prisonVisitSlot)

    officialVisitRepository.saveAndFlush(
      OfficialVisitEntity(
        prisonVisitSlot = prisonVisitSlot,
        prisonCode = prisonCode,
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
        searchTypeCode = request.searchTypeCode,
        createdBy = user.username,
      ).addVisitorsAndAnyEquipment(request.officialVisitors, matchingVisitors, user),
    ).savePrisonerBeingVisited()
      .let {
        CreateOfficialVisitResponse(it.officialVisitId)
      }.also {
        logger.info("Official visit created with ID ${it.officialVisitId}")
      }
  }

  private fun OfficialVisitEntity.addVisitorsAndAnyEquipment(officialVisitors: List<OfficialVisitor>, matchingVisitors: List<ApprovedContact>, user: User) = apply {
    officialVisitors.forEach { ov ->
      val matchingVisitor = matchingVisitors.single { mv -> mv.contactId == ov.contactId && mv.prisonerContactId == ov.prisonerContactId }

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

  private fun OfficialVisitEntity.savePrisonerBeingVisited() = also {
    prisonerVisitedRepository.saveAndFlush(
      PrisonerVisitedEntity(
        officialVisit = it,
        prisonerNumber = it.prisonerNumber,
        attendanceCode = null,
        createdBy = it.createdBy,
      ),
    )
  }

  private fun CreateOfficialVisitRequest.checkVisitDateAndTimes() = also {
    require(visitDate!!.atTime(startTime) > now()) { "Official visit cannot be scheduled in the past" }
    require(startTime!! < endTime) { "Official visit start time must be before end time" }
  }

  private fun CreateOfficialVisitRequest.checkStillAvailable(prisonCode: String, prisonVisitSlot: PrisonVisitSlotEntity) = also {
    val availableSlots = availableSlotService.getAvailableSlotsForPrison(
      prisonCode = prisonCode,
      fromDate = visitDate!!,
      toDate = visitDate,
      videoOnly = visitTypeCode!! == VisitType.VIDEO,
    )

    require(
      availableSlots.any { it.visitSlotId == prisonVisitSlot.prisonVisitSlotId && it.startTime == startTime && it.dpsLocationId == dpsLocationId },
    ) {
      "Prison visit slot ${prisonVisitSlot.prisonVisitSlotId} is no longer available for the requested date and time."
    }
  }

  private fun CreateOfficialVisitRequest.getPrisonersDetails(prisonCode: String) = run {
    prisonerValidator.validatePrisonerAtPrison(prisonerNumber!!, prisonCode)
  }

  private fun CreateOfficialVisitRequest.getVisitorDetails() = run {
    require(officialVisitors.isNotEmpty()) { "At least one official visitor must be supplied." }

    val requestedVisitors = officialVisitors.filter { it.visitorTypeCode == VisitorType.CONTACT }.toSet()
    val approvedVisitors = contactsService.getApprovedContacts(prisonerNumber!!)

    requestedVisitors.map { requestedVisitor ->
      val matchingVisitor = approvedVisitors.singleOrNull { approvedVisitor -> approvedVisitor.contactId == requestedVisitor.contactId && approvedVisitor.prisonerContactId == requestedVisitor.prisonerContactId }

      requireNotNull(matchingVisitor) {
        "Visitor with contact ID ${requestedVisitor.contactId} and prisoner contact ID ${requestedVisitor.prisonerContactId} is not approved for visiting prisoner number $prisonerNumber."
      }

      matchingVisitor
    }
  }
}
