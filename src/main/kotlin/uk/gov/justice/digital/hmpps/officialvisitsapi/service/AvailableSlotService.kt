package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AvailableSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitBookedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AvailableSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitBookedRepository
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class AvailableSlotService(
  private val timeSource: TimeSource,
  private val visitBookedRepository: VisitBookedRepository,
  private val availableSlotRepository: AvailableSlotRepository,
  private val locationService: LocationsService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getAvailableSlotsForPrison(prisonCode: String, fromDate: LocalDate, toDate: LocalDate, videoOnly: Boolean) = run {
    require(fromDate >= timeSource.today()) { "The from date must be on or after today's date" }
    require(toDate >= fromDate) { "The to date must be on or after the from date" }

    val availableSlots = getAvailableSlots(prisonCode, videoOnly)
    val bookedSlots = visitBookedRepository.findCurrentVisitsBookedBy(prisonCode, fromDate, toDate)

    val sortedSlots = AvailableSlotBuilder.builder(timeSource, fromDate, toDate) { bookedSlots.forEach(::add) }.build(availableSlots, videoOnly)
      .sortedWith(compareBy({ it.visitDate }, { it.startTime }))

    decorateWithLocationDescription(prisonCode, sortedSlots)
  }

  private fun getAvailableSlots(prisonCode: String, videoOnly: Boolean) = if (videoOnly) {
    availableSlotRepository.findAvailableVideoSlotsByPrisonCode(prisonCode)
  } else {
    availableSlotRepository.findAvailableSlotsByPrisonCode(prisonCode)
  }

  private fun decorateWithLocationDescription(prisonCode: String, slots: List<AvailableSlot>): List<AvailableSlot> {
    val activeVisitLocations = locationService.getOfficialVisitLocationsAtPrison(prisonCode)
    log.info("Found ${slots.size} visit locations for prison $prisonCode")

    val decoratedSlots = slots.map { slot ->
      val location = activeVisitLocations?.find { location -> location.id == slot.dpsLocationId }
      if (location == null) {
        log.info("Unmatched location for visit ${slot.dpsLocationId} for $prisonCode is not in locations list")
      } else {
        slot.locationDescription = location.localName
        log.info("Matched location for visit ${slot.dpsLocationId} for $prisonCode found ${location.localName}")
      }
      slot
    }

    return decoratedSlots
  }
}

private class AvailableSlotBuilder private constructor(private val timeSource: TimeSource, private val fromDate: LocalDate, private val toDate: LocalDate) {
  companion object {
    fun builder(timeSource: TimeSource, from: LocalDate, to: LocalDate, init: AvailableSlotBuilder.() -> Unit) = AvailableSlotBuilder(timeSource, from, to).also { it.init() }
  }

  private val datedInPersonVisits = mutableMapOf<DatedVisit, Int>()
  private val datedVideoVisits = mutableMapOf<DatedVisit, Int>()

  fun add(bookedSlot: VisitBookedEntity) {
    val key = DatedVisit(bookedSlot)

    when {
      bookedSlot.isVisitType(VisitType.IN_PERSON) -> {
        datedInPersonVisits.computeIfPresent(key) { _, count -> count + 1 }
        datedInPersonVisits.computeIfAbsent(key) { 1 }
      }
      bookedSlot.isVisitType(VisitType.VIDEO) -> {
        datedVideoVisits.computeIfPresent(key) { _, count -> count + 1 }
        datedVideoVisits.computeIfAbsent(key) { 1 }
      }
    }
  }

  fun build(availableSlots: List<AvailableSlotEntity>, videoOnly: Boolean) = run {
    val results = buildList {
      availableSlots.forEach { availableSlot ->
        for (date in fromDate..toDate) {
          // Only add to the list if the slot is on the same day as the date in question and there is capacity, otherwise ignore the slot.
          if (availableSlot.isOnSameDay(date.dayOfWeek)) {
            val availableAdults = availableSlot.availableAdultsOn(date)
            val availableGroups = availableSlot.availableGroupsOn(date)
            val availableVideoSessions = availableSlot.availableVideoSessionsOn(date)

            if ((availableAdults > 0 && availableGroups > 0 && !videoOnly) || (availableGroups > 0 && availableVideoSessions > 0)) {
              add(
                AvailableSlot(
                  visitSlotId = availableSlot.prisonVisitSlotId,
                  timeSlotId = availableSlot.prisonTimeSlotId,
                  prisonCode = availableSlot.prisonCode,
                  dayCode = availableSlot.dayCode,
                  dayDescription = availableSlot.dayDescription,
                  visitDate = date,
                  startTime = availableSlot.startTime,
                  endTime = availableSlot.endTime,
                  dpsLocationId = availableSlot.dpsLocationId,
                  availableVideoSessions = availableVideoSessions,
                  availableAdults = if (videoOnly) (availableSlot.maxAdults ?: 0) else availableAdults,
                  availableGroups = availableGroups,
                ),
              )
            }
          }
        }
      }
    }

    // Filter out anything with a start time earlier than now, there should only ever be a handful or less
    results.filter { it.visitDate.atTime(it.startTime) > timeSource.now() }
  }

  private fun AvailableSlotEntity.isOnSameDay(dayOfWeek: DayOfWeek) = Day.valueOf(dayCode).value == dayOfWeek.value

  private fun AvailableSlotEntity.availableAdultsOn(date: LocalDate) = (maxAdults ?: 0) - datedInPersonVisits.individualVisitCount(date, this)

  private fun AvailableSlotEntity.availableGroupsOn(date: LocalDate) = (maxGroups ?: 0) - datedInPersonVisits.groupVisitCount(date, this) - datedVideoVisits.groupVisitCount(date, this)

  private fun AvailableSlotEntity.availableVideoSessionsOn(date: LocalDate) = (maxVideoSessions ?: 0) - datedVideoVisits.individualVisitCount(date, this)

  private fun Map<DatedVisit, Int>.groupVisitCount(date: LocalDate, slot: AvailableSlotEntity) = run {
    count { dsc -> dsc.key.date == date && dsc.key.prisonTimeSlotId == slot.prisonTimeSlotId && dsc.key.prisonVisitSlotId == slot.prisonVisitSlotId }
  }

  private fun Map<DatedVisit, Int>.individualVisitCount(date: LocalDate, slot: AvailableSlotEntity) = run {
    filter { dsc -> dsc.key.date == date && dsc.key.prisonTimeSlotId == slot.prisonTimeSlotId && dsc.key.prisonVisitSlotId == slot.prisonVisitSlotId }
      .map { it.value }
      .sum()
  }
}

enum class Day(val value: Int) {
  MON(1),
  TUE(2),
  WED(3),
  THU(4),
  FRI(5),
  SAT(6),
  SUN(7),
}

private data class DatedVisit(
  val date: LocalDate,
  val officialVisitId: Long,
  val prisonTimeSlotId: Long,
  val prisonVisitSlotId: Long,
) {
  constructor(vbe: VisitBookedEntity) : this(
    date = vbe.visitDate,
    officialVisitId = vbe.officialVisitId,
    prisonTimeSlotId = vbe.prisonTimeSlotId,
    prisonVisitSlotId = vbe.prisonVisitSlotId,
  )
}

class LocalDateIterator(
  val start: LocalDate,
  val endInclusive: LocalDate,
  val stepDays: Long = 1,
) : Iterable<LocalDate> {
  override fun iterator(): Iterator<LocalDate> = object : Iterator<LocalDate> {
    private var current = start

    override fun hasNext() = if (stepDays > 0) current <= endInclusive else current >= endInclusive

    override fun next() = run {
      val next = current

      current = current.plusDays(stepDays)

      next
    }
  }
}

operator fun LocalDate.rangeTo(other: LocalDate) = LocalDateIterator(this, other)
