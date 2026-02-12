package uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toTimeSlotListModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toVisitSlotListModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummaryItem
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class PrisonTimeSlotService(
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val locationService: LocationsService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getPrisonTimeSlotById(prisonTimeSlotId: Long): TimeSlot {
    val prisonTimeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    return prisonTimeSlotEntity.toModel()
  }

  fun create(request: CreateTimeSlotRequest, user: User): TimeSlot {
    request.validate()
    return prisonTimeSlotRepository.saveAndFlush(request.toEntity(user.username)).toModel()
  }

  fun update(prisonTimeSlotId: Long, request: UpdateTimeSlotRequest, user: User): TimeSlot {
    val timeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    request.validate()
    val changedTimeSlotEntity = timeSlotEntity.copy(
      dayCode = request.dayCode,
      startTime = request.startTime,
      endTime = request.endTime,
      effectiveDate = request.effectiveDate,
      expiryDate = request.expiryDate,
      updatedBy = user.username,
      updatedTime = LocalDateTime.now(),
    )
    return prisonTimeSlotRepository.saveAndFlush(changedTimeSlotEntity).toModel()
  }

  fun delete(prisonTimeSlotId: Long): TimeSlot {
    val deleted = prisonTimeSlotRepository.findById(prisonTimeSlotId).orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    // check association with visit slot
    require(noVisitSlotsExistFor(prisonTimeSlotId)) {
      throw EntityInUseException("The prison time slot has one or more visit slots associated with it and cannot be deleted.")
    }
    prisonTimeSlotRepository.deleteById(prisonTimeSlotId)
    return deleted.toModel()
  }

  private fun noVisitSlotsExistFor(prisonTimeSlotId: Long): Boolean = !prisonVisitSlotRepository.existsByPrisonTimeSlotId(prisonTimeSlotId)

  private fun UpdateTimeSlotRequest.validate() = also {
    require(startTime < endTime) { "Prison time slot start time must be before end time" }
    require(expiryDate == null || expiryDate >= LocalDate.now()) { "Prison time slot expiry date must not be in the past" }
  }

  private fun CreateTimeSlotRequest.validate() = also {
    require(startTime < endTime) { "Prison time slot start time must be before end time" }
    require(expiryDate == null || expiryDate >= LocalDate.now()) { "Prison time slot expiry date must not be in the past" }
  }

  fun getAllPrisonTimeSlotsAndAssociatedVisitSlots(prisonCode: String, activeOnly: Boolean): TimeSlotSummary {
    val timeSlots = if (activeOnly) {
      prisonTimeSlotRepository.findAllActiveByPrisonCode(prisonCode)
    } else {
      prisonTimeSlotRepository.findAllByPrisonCode(prisonCode)
    }.toTimeSlotListModel()

    val timeSlotIds = timeSlots.map { it.prisonTimeSlotId }
    val visitSlots: List<VisitSlot> =
      if (timeSlotIds.isEmpty()) {
        emptyList()
      } else {
        prisonVisitSlotRepository.findByPrisonTimeSlotIdIn(timeSlotIds)
          .toVisitSlotListModel(prisonCode)
      }

    val decoratedVisitSlots = decorateWithLocationDescription(
      prisonCode,
      slots = visitSlots,
    )

    val visitSlotByTimeSlotIds: Map<Long, List<VisitSlot>> = decoratedVisitSlots.groupBy { it.prisonTimeSlotId }

    val prisonName = prisonerSearchClient.findPrisonName(prisonCode)

    return TimeSlotSummary(
      prisonCode = prisonCode,
      timeSlots = timeSlots.map { ts ->
        TimeSlotSummaryItem(
          timeSlot = ts,
          visitSlots = visitSlotByTimeSlotIds[ts.prisonTimeSlotId].orEmpty(),
        )
      },
      prisonName = prisonName,
    )
  }

  private fun decorateWithLocationDescription(prisonCode: String, slots: List<VisitSlot>): List<VisitSlot> {
    val activeVisitLocations = locationService.getOfficialVisitLocationsAtPrison(prisonCode)
    log.info("Found ${activeVisitLocations.size} official visit locations for prison $prisonCode")

    val locationById = activeVisitLocations.associateBy { it.id }

    val decoratedSlots = slots.map { slot ->
      val location = locationById[slot.dpsLocationId]
      if (location == null) {
        log.warn("Location ${slot.dpsLocationId} for visit slot ${slot.visitSlotId} in prison $prisonCode is not found in the official visit locations list")
        slot.copy(locationDescription = "** unknown **")
      } else {
        slot.copy(
          locationDescription = location.localName,
          locationMaxCapacity = getCapacityForVisitType(location),
          locationType = location.locationType.value,
        )
      }
    }

    return decoratedSlots
  }

  private fun getCapacityForVisitType(location: Location): Int? = location.usage?.firstNotNullOfOrNull { dto -> if (dto.usageType.name == "VISIT") dto.capacity else null }
}
