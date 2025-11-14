package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AvailableSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitBookedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AvailableSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitBookedRepository
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class AvailableSlotService(
  private val visitBookedRepository: VisitBookedRepository,
  private val availableSlotRepository: AvailableSlotRepository,
) {
  fun getAvailableSlotsForPrison(prisonCode: String, fromDate: LocalDate, toDate: LocalDate, videoOnly: Boolean) = run {
    require(fromDate >= LocalDate.now()) { "The from date must be on or after today's date" }
    require(toDate >= fromDate) { "The to date must be on or after the from date" }

    val availableSlots = availableSlotRepository.findAvailableSlotsByPrisonCode(prisonCode)
    val bookedSlots =
      visitBookedRepository.findVisitBookedEntityByPrisonCodeAndVisitDateBetween(prisonCode, fromDate, toDate)

    AvailableSlotBuilder.builder(fromDate, toDate) { bookedSlots.forEach(::add) }.build(availableSlots)
  }
}

private class AvailableSlotBuilder private constructor(private val fromDate: LocalDate, private val toDate: LocalDate) {
  companion object {
    fun builder(from: LocalDate, to: LocalDate, init: AvailableSlotBuilder.() -> Unit) = AvailableSlotBuilder(from, to).also { it.init() }
  }

  private val datedVisitCounts = mutableMapOf<DatedVisit, Int>()

  fun add(bookedSlot: VisitBookedEntity) {
    val key = DatedVisit(
      bookedSlot.visitDate,
      officialVisitId = bookedSlot.officialVisitId,
      bookedSlot.prisonTimeSlotId,
      bookedSlot.prisonVisitSlotId,
    )

    if (datedVisitCounts.containsKey(key)) {
      datedVisitCounts[key] = datedVisitCounts[key]!! + 1
    } else {
      datedVisitCounts[key] = 1
    }
  }

  fun build(availableSlots: List<AvailableSlotEntity>) = run {
    val results = buildList {
      availableSlots.forEach { availableSlot ->
        for (date in fromDate..toDate) {
          // Only add to the list if the slot is on the same day as the date in question, otherwise ignore the slot.
          if (availableSlot.isOnSameDay(date.dayOfWeek)) {
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
                availableVideoSessions = 0,
                availableAdults = availableSlot.maxAdults - datedVisitCounts.adultCount(date, availableSlot),
                availableGroups = availableSlot.maxGroups - datedVisitCounts.groupCount(date, availableSlot),
              ),
            )
          }
        }
      }
    }

    results
  }

  private fun AvailableSlotEntity.isOnSameDay(dayOfWeek: DayOfWeek) = Day.valueOf(dayCode).value == dayOfWeek.value

  private fun Map<DatedVisit, Int>.groupCount(date: LocalDate, slot: AvailableSlotEntity) = run {
    count { dsc -> dsc.key.date == date && dsc.key.prisonTimeSlotId == slot.prisonTimeSlotId && dsc.key.prisonVisitSlotId == slot.prisonVisitSlotId }
  }

  private fun Map<DatedVisit, Int>.adultCount(date: LocalDate, slot: AvailableSlotEntity) = run {
    filter { dsc -> dsc.key.date == date && dsc.key.prisonTimeSlotId == slot.prisonTimeSlotId && dsc.key.prisonVisitSlotId == slot.prisonVisitSlotId }
      .map { it.value }
      .sum()
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
}

private data class DatedVisit(
  val date: LocalDate,
  val officialVisitId: Long,
  val prisonTimeSlotId: Long,
  val prisonVisitSlotId: Long,
)

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
