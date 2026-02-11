package uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toTimeSlotListModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toVisitSlotListModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummaryItem
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService

@Service
@Transactional(readOnly = true)
class AdminService(
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val locationService: LocationsService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
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

    val prisoners = prisonCode?.let { prisonerSearchClient.findPrisonersBySearchTerm(prisonCode, searchTerm = "") }
      ?: emptyList()

    return TimeSlotSummary(
      prisonCode = prisonCode,
      timeSlots = timeSlots.map { ts ->
        TimeSlotSummaryItem(
          timeSlot = ts,
          visitSlots = visitSlotByTimeSlotIds[ts.prisonTimeSlotId].orEmpty(),
        )
      },
      prisonName = prisoners[0].prisonName ?: "Unknown prison name",
    )
  }

  private fun decorateWithLocationDescription(prisonCode: String, slots: List<VisitSlot>): List<VisitSlot> {
    val activeVisitLocations = locationService.getOfficialVisitLocationsAtPrison(prisonCode)
    log.info("Found ${slots.size} official visit locations for prison $prisonCode")

    val decoratedSlots = slots.map { slot ->
      val location = activeVisitLocations.find { location -> location.id == slot.dpsLocationId }
      if (location == null) {
        log.error("Unmatched location for visit ${slot.dpsLocationId} for $prisonCode is not in the official visits locations")
        slot.copy(locationDescription = "** unknown **")
      } else {
        slot.copy(
          locationDescription = location.localName,
          locationCapacity = location.capacity?.maxCapacity,
        ) // TODO check if we should using working capacity here
      }
    }

    return decoratedSlots
  }
}
