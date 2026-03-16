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
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditCreateEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OfficialVisitMetricTelemetryService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitMetricInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.slotavailability.AvailableSlotService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.slotavailability.AvailableSlotSpecification
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.slotavailability.AvailableSlotSpecificationFactory
import java.time.LocalDateTime.now

@Service
class OfficialVisitCreateService(
  private val prisonerValidator: PrisonerValidator,
  private val availableSlotService: AvailableSlotService,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val contactsService: ContactsService,
  private val auditingService: AuditingService,
  val officialVisitMetricTelemetryService: OfficialVisitMetricTelemetryService,
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
        visitTypeCode = request.visitTypeCode!!,
        staffNotes = request.staffNotes,
        prisonerNotes = request.prisonerNotes,
        offenderBookId = prisonerDetails.bookingId?.toLong(),
        createdBy = user.username,
      ).addVisitorsAndAnyEquipment(request.officialVisitors, matchingVisitors, user),
    ).savePrisonerBeingVisited()
      .let { it ->
        CreateOfficialVisitResponse(
          officialVisitId = it.officialVisitId,
          prisonerNumber = it.prisonerNumber,
          visitorAndContactIds = it.officialVisitors().map { visitor -> visitor.officialVisitorId to visitor.contactId },
        )
      }.also {
        logger.info("Official visit created with ID ${it.officialVisitId}")
      }.also {
        officialVisitMetricTelemetryService.send(
          MetricsEvents.CREATE,
          VisitMetricInfo(
            username = user.username,
            officialVisitId = it.officialVisitId,
            prisonCode = prisonCode,
            prisonerNumber = it.prisonerNumber,
            numberOfVisitors = it.visitorAndContactIds.size.toLong(),
            startTime = request.startTime,
          ),
        )
      }.also {
        auditingService.recordAuditEvent(
          auditCreateEvent {
            officialVisitId(it.officialVisitId)
            summaryText("Official visit created")
            eventSource("DPS")
            user(user)
            prisonCode(prisonCode)
            prisonerNumber(it.prisonerNumber)
            detailsText(
              "Official visit created for prisoner number ${it.prisonerNumber} with ${it.visitorAndContactIds.size} visitor(s)",
            )
          },
        )
      }
  }

  private fun OfficialVisitEntity.addVisitorsAndAnyEquipment(officialVisitors: List<OfficialVisitor>, matchingVisitors: List<PrisonerContact>, user: User) = apply {
    officialVisitors.forEach { ov ->
      // As we are creating a visit locally in DPS the check for contactId and prisonerContactId is fine here
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

    val availableSlotSpecification: AvailableSlotSpecification = AvailableSlotSpecificationFactory.getAvailableSlotSpecification(this)

    require(availableSlots.any { slot -> availableSlotSpecification.isSatisfiedBy(slot, prisonVisitSlot) }) {
      "Prison visit slot ${prisonVisitSlot.prisonVisitSlotId} is not available for the requested date, time and number of visitors."
    }
  }

  private fun CreateOfficialVisitRequest.getPrisonersDetails(prisonCode: String) = run {
    prisonerValidator.validatePrisonerAtPrison(prisonerNumber!!, prisonCode)
  }

  private fun CreateOfficialVisitRequest.getVisitorDetails() = run {
    require(officialVisitors.isNotEmpty()) { "At least one official visitor must be supplied." }

    val requestedVisitors = officialVisitors.filter { it.visitorTypeCode == VisitorType.CONTACT }.toSet()

    // This will intentionally allow either active or inactive contacts - as long as they are currentTerm=true and approved=true
    val approvedVisitors = contactsService.getAllPrisonerContacts(prisonerNumber = prisonerNumber!!, approved = true, currentTerm = true)

    requestedVisitors.map { requestedVisitor ->
      // As we are creating a visit locally in DPS the check for contactId and prisonerContactId is fine here
      val matchingVisitor = approvedVisitors.singleOrNull { approvedVisitor -> approvedVisitor.contactId == requestedVisitor.contactId && approvedVisitor.prisonerContactId == requestedVisitor.prisonerContactId }

      requireNotNull(matchingVisitor) {
        "Visitor with contact ID ${requestedVisitor.contactId} and prisoner contact ID ${requestedVisitor.prisonerContactId} is not approved for visiting prisoner number $prisonerNumber."
      }

      matchingVisitor
    }
  }
}
