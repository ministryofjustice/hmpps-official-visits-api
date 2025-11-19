package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.stub
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AvailableSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitBookedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AvailableSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitBookedRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class AvailableSlotServiceTest {
  private val visitBookedRepository: VisitBookedRepository = mock()
  private val availableSlotRepository: AvailableSlotRepository = mock()
  private lateinit var availableSlotService: AvailableSlotService

  @Nested
  inner class BadDates {
    @BeforeEach
    fun beforeEach() {
      availableSlotService = service(LocalDateTime.now())
    }

    @Test
    fun `should fail if from date is in the past`() {
      val exception = assertThrows<IllegalArgumentException> {
        availableSlotService.getAvailableSlotsForPrison(MOORLAND, today().minusDays(1), today(), false)
      }

      exception.message isEqualTo "The from date must be on or after today's date"
    }

    @Test
    fun `should fail if to date is before from date`() {
      val exception = assertThrows<IllegalArgumentException> {
        availableSlotService.getAvailableSlotsForPrison(MOORLAND, today(), today().minusDays(1), false)
      }

      exception.message isEqualTo "The to date must be on or after the from date"
    }
  }

  @Nested
  @DisplayName("Available slots from Monday afternoon onwards, it does not factor in booked slots")
  inner class AvailableSlotsFromMondayAfternoonOnwardsNoBookedSlots {
    private val mondayAtMidday = LocalDate.of(2025, 11, 17).atTime(12, 0)
    private val availableSlots: MutableList<AvailableSlotEntity> = mutableListOf()
    private val bookedSlots: MutableList<VisitBookedEntity> = mutableListOf()

    @BeforeEach
    fun beforeEach() {
      availableSlotService = service(mondayAtMidday)
      availableSlotRepository.stub { on { findAvailableSlotsByPrisonCode(MOORLAND) } doReturn availableSlots }
      visitBookedRepository.stub {
        on {
          findCurrentVisitsBookedBy(
            eq(MOORLAND),
            eq(mondayAtMidday.toLocalDate()),
            any(),
          )
        } doReturn bookedSlots
      }
    }

    @Test
    fun `should be 3 available slots on Monday afternoon`() {
      availableSlots.add(availableSlot(Day.MON, 13))
      availableSlots.add(availableSlot(Day.MON, 14))
      availableSlots.add(availableSlot(Day.MON, 15))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate(),
          false,
        )

      freeSlots.size isEqualTo 3
    }

    @Test
    fun `should be no available slots on Monday afternoon`() {
      availableSlots.add(availableSlot(Day.MON, 9))
      availableSlots.add(availableSlot(Day.MON, 10))
      availableSlots.add(availableSlot(Day.MON, 11))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate(),
          false,
        )

      freeSlots.size isEqualTo 0
    }

    @Test
    fun `should be 3 available slots on Monday the following week`() {
      availableSlots.add(availableSlot(Day.MON, 9))
      availableSlots.add(availableSlot(Day.MON, 10))
      availableSlots.add(availableSlot(Day.MON, 11))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusWeeks(1),
          false,
        )

      freeSlots.size isEqualTo 3
    }

    @Test
    fun `should be one available slot on Monday afternoon`() {
      availableSlots.add(availableSlot(Day.MON, 11))
      availableSlots.add(availableSlot(Day.MON, 12))
      availableSlots.add(availableSlot(Day.MON, 13))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate(),
          false,
        )

      freeSlots.size isEqualTo 1
    }

    @Test
    fun `should be no available slots on Monday`() {
      availableSlots.add(availableSlot(Day.TUE, 10))
      availableSlots.add(availableSlot(Day.TUE, 11))
      availableSlots.add(availableSlot(Day.TUE, 12))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate(),
          false,
        )

      freeSlots.size isEqualTo 0
    }

    @Test
    fun `should be one available slot on Tuesday morning`() {
      availableSlots.add(availableSlot(Day.MON, 10))
      availableSlots.add(availableSlot(Day.MON, 11))
      availableSlots.add(availableSlot(Day.TUE, 11))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusDays(1),
          false,
        )

      freeSlots.size isEqualTo 1
    }
  }

  @Nested
  @DisplayName("Available slots from Monday afternoon onwards when slots are booked")
  inner class AvailableSlotsFromMondayAfternoonOnwardsWhenSlotsBooked {
    private val mondayAtMidday = LocalDate.of(2025, 11, 17).atTime(12, 0)
    private val availableSlots: MutableList<AvailableSlotEntity> = mutableListOf()
    private val bookedSlots: MutableList<VisitBookedEntity> = mutableListOf()

    @BeforeEach
    fun beforeEach() {
      availableSlotService = service(mondayAtMidday)
      availableSlotRepository.stub { on { findAvailableSlotsByPrisonCode(MOORLAND) } doReturn availableSlots }
      visitBookedRepository.stub {
        on {
          findCurrentVisitsBookedBy(
            eq(MOORLAND),
            eq(mondayAtMidday.toLocalDate()),
            any(),
          )
        } doReturn bookedSlots
      }
    }

    @Test
    fun `should be one available slot on Monday afternoon when only one slot free`() {
      availableSlots.add(availableSlot(Day.MON, 13))
      availableSlots.add(availableSlot(Day.MON, 14))
      availableSlots.add(availableSlot(Day.MON, 15, 5, 5))

      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(1)))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(2)))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusDays(1),
          false,
        )

      freeSlots
        .single()
        .availableAdultsIsEqualTo(5)
        .availableGroupsIsEqualTo(5)
    }

    @Test
    fun `should be no available slots on Monday when fully booked`() {
      availableSlots.add(availableSlot(Day.MON, 13))
      availableSlots.add(availableSlot(Day.MON, 14))
      availableSlots.add(availableSlot(Day.MON, 15))

      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(1)))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(2)))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3)))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusDays(1),
          false,
        )

      freeSlots.size isEqualTo 0
    }

    @Test
    fun `should be one available slot on Monday afternoon when only one slot free with reduced capacity`() {
      availableSlots.add(availableSlot(Day.MON, 15, 5, 2))

      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3)))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3)))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusDays(1),
          false,
        )

      freeSlots
        .single()
        .availableAdultsIsEqualTo(3)
        .availableGroupsIsEqualTo(1)
    }
  }

  private fun service(dateTime: LocalDateTime) = AvailableSlotService({ dateTime }, visitBookedRepository, availableSlotRepository)

  private fun availableSlot(day: Day, startHour: Int, maxAdults: Int = 1, maxGroups: Int = 1) = AvailableSlotEntity(
    prisonVisitSlotId = startHour.toLong(),
    prisonTimeSlotId = startHour.toLong(),
    prisonCode = MOORLAND,
    displaySequence = -1,
    dayCode = day.toString(),
    dayDescription = day.toString(),
    startTime = LocalTime.of(startHour, 0),
    endTime = LocalTime.of(startHour, 59),
    dpsLocationId = UUID.randomUUID(),
    maxAdults = maxAdults,
    maxGroups = maxGroups,
    maxVideoSessions = 1,
  )

  private fun bookedSlot(dateTime: LocalDateTime) = VisitBookedEntity(
    officialVisitId = -1,
    prisonCode = MOORLAND,
    dayCode = dateTime.dayOfWeek.toString().take(3).uppercase(),
    dayDescription = dateTime.dayOfWeek.toString(),
    prisonVisitSlotId = dateTime.toLocalTime().hour.toLong(),
    prisonTimeSlotId = dateTime.toLocalTime().hour.toLong(),
    visitDate = dateTime.toLocalDate(),
    startTime = dateTime.toLocalTime(),
    endTime = dateTime.toLocalTime(),
    visitStatusCode = "--",
    visitTypeCode = "--",
    prisonerNumber = "--",
    contactId = -1,
    visitorTypeCode = "--",
    contactTypeCode = "--",
    relationshipCode = "--",
    firstName = "first name",
    lastName = "last name",
    dpsLocationId = UUID.randomUUID(),
  )

  private fun AvailableSlot.availableAdultsIsEqualTo(expected: Int) = also { availableAdults isEqualTo expected }
  private fun AvailableSlot.availableGroupsIsEqualTo(expected: Int) = also { availableGroups isEqualTo expected }
}
