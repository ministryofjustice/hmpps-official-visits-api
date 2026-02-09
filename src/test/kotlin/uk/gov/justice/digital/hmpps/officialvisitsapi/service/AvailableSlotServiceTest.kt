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
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AvailableSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitBookedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
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
  private val locationsService: LocationsService = mock()
  private lateinit var availableSlotService: AvailableSlotService

  @Nested
  inner class BadDates {
    @BeforeEach
    fun beforeEach() {
      availableSlotService = service(LocalDateTime.now())

      // This is only used to decorate available slots with location descriptions
      locationsService.stub {
        on {
          getOfficialVisitLocationsAtPrison(
            eq(MOORLAND),
          )
        } doReturn officialVisitLocations()
      }
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

      availableSlotRepository.stub {
        on {
          findAvailableSlotsForPrison(MOORLAND, mondayAtMidday.toLocalDate())
        } doReturn availableSlots
      }

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
      availableSlots.add(availableSlot(Day.MON, 13, effectiveDate = mondayAtMidday.toLocalDate().minusDays(1)))
      availableSlots.add(availableSlot(Day.MON, 14, effectiveDate = mondayAtMidday.toLocalDate().minusDays(1)))
      availableSlots.add(availableSlot(Day.MON, 15, effectiveDate = mondayAtMidday.toLocalDate().minusDays(1)))

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
      availableSlots.add(availableSlot(Day.MON, 9, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 10, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 11, effectiveDate = mondayAtMidday.toLocalDate()))

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
      availableSlots.add(availableSlot(Day.MON, 9, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 10, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 11, effectiveDate = mondayAtMidday.toLocalDate()))

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
    fun `should be 1 free slot with 0 available video, 1 group and 1 adult on Monday afternoon`() {
      availableSlots.add(availableSlot(Day.MON, 11, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 12, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 13, effectiveDate = mondayAtMidday.toLocalDate()))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate(),
          false,
        )

      freeSlots
        .single()
        .dateIsEqualTo(mondayAtMidday.toLocalDate())
        .dayIsEqualTo(Day.MON)
        .startTimeIsEqual(LocalTime.of(13, 0))
        .availableAdultsIsEqualTo(1)
        .availableGroupsIsEqualTo(1)
        .availableVideosIsEqualTo(0)
    }

    @Test
    fun `should be no available slots on Monday`() {
      availableSlots.add(availableSlot(Day.TUE, 10, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.TUE, 11, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.TUE, 12, effectiveDate = mondayAtMidday.toLocalDate()))

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
    fun `should be 1 free slot with 0 available video, 1 group and 1 adult on Tuesday morning`() {
      availableSlots.add(availableSlot(Day.MON, 10, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 11, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.TUE, 11, effectiveDate = mondayAtMidday.toLocalDate()))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusDays(1),
          false,
        )

      freeSlots
        .single()
        .dateIsEqualTo(mondayAtMidday.toLocalDate().plusDays(1))
        .dayIsEqualTo(Day.TUE)
        .startTimeIsEqual(LocalTime.of(11, 0))
        .availableAdultsIsEqualTo(1)
        .availableGroupsIsEqualTo(1)
        .availableVideosIsEqualTo(0)
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
      availableSlotRepository.stub { on { findAvailableSlotsForPrison(MOORLAND, mondayAtMidday.toLocalDate()) } doReturn availableSlots }
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
    fun `should be 1 free slot with 0 available video, 5 groups and 5 adults on Monday afternoon`() {
      availableSlots.add(availableSlot(Day.MON, 13, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 14, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 15, 5, 5, effectiveDate = mondayAtMidday.toLocalDate()))

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
        .dateIsEqualTo(mondayAtMidday.toLocalDate())
        .dayIsEqualTo(Day.MON)
        .startTimeIsEqual(LocalTime.of(15, 0))
        .availableAdultsIsEqualTo(5)
        .availableGroupsIsEqualTo(5)
        .availableVideosIsEqualTo(0)
    }

    @Test
    fun `should be no available slots on Monday when fully booked`() {
      availableSlots.add(availableSlot(Day.MON, 13, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 14, effectiveDate = mondayAtMidday.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 15, effectiveDate = mondayAtMidday.toLocalDate()))

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
    fun `should be 1 free slot with 2 available video, 1 group and 3 adults on Monday afternoon`() {
      availableSlots.add(availableSlot(Day.MON, 15, 5, 2, maxVideo = 2, effectiveDate = mondayAtMidday.toLocalDate()))

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
        .dateIsEqualTo(mondayAtMidday.toLocalDate())
        .dayIsEqualTo(Day.MON)
        .startTimeIsEqual(LocalTime.of(15, 0))
        .availableAdultsIsEqualTo(3)
        .availableGroupsIsEqualTo(1)
        .availableVideosIsEqualTo(2)
    }

    @Test
    fun `should be no available slot on Monday afternoon when available adult capacity met`() {
      availableSlots.add(availableSlot(Day.MON, 15, 5, 2, effectiveDate = mondayAtMidday.toLocalDate()))

      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3)))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3)))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3)))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3)))
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
    fun `should be no available slot on Monday afternoon when available group capacity met`() {
      availableSlots.add(availableSlot(Day.MON, 15, 5, 2, effectiveDate = mondayAtMidday.toLocalDate()))

      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3), 1))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3), 2))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusDays(1),
          false,
        )

      freeSlots.size isEqualTo 0
    }
  }

  @Nested
  @DisplayName("Available video slots from Monday afternoon onwards when slots are booked")
  inner class AvailableVideoSlotsFromMondayAfternoonOnwardsWhenSlotsBooked {
    private val mondayAtMidday = LocalDate.of(2025, 11, 17).atTime(12, 0)
    private val availableSlots: MutableList<AvailableSlotEntity> = mutableListOf()
    private val bookedSlots: MutableList<VisitBookedEntity> = mutableListOf()

    @BeforeEach
    fun beforeEach() {
      availableSlotService = service(mondayAtMidday)
      availableSlotRepository.stub { on { findAvailableVideoSlotsForPrison(MOORLAND, mondayAtMidday.toLocalDate()) } doReturn availableSlots }
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
    fun `should be 1 free slot with 1 available video, 3 groups and 5 adults on Monday afternoon`() {
      availableSlots.add(availableSlot(Day.MON, 14, 5, 5, 3, effectiveDate = mondayAtMidday.toLocalDate()))

      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(2), officialVisitId = 1, videoOnly = true))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(2), officialVisitId = 2, videoOnly = true))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusDays(1),
          true,
        )

      freeSlots
        .single()
        .dateIsEqualTo(mondayAtMidday.toLocalDate())
        .dayIsEqualTo(Day.MON)
        .startTimeIsEqual(LocalTime.of(14, 0))
        .availableAdultsIsEqualTo(5)
        .availableGroupsIsEqualTo(3)
        .availableVideosIsEqualTo(1)
    }

    @Test
    fun `should be 1 free slot with 1 available video, 4 groups and 5 adults on Monday afternoon`() {
      availableSlots.add(availableSlot(Day.MON, 15, 5, 5, 3, effectiveDate = mondayAtMidday.toLocalDate()))

      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3), officialVisitId = 1, videoOnly = true))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3), officialVisitId = 1, videoOnly = true))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusDays(1),
          true,
        )

      freeSlots
        .single()
        .dateIsEqualTo(mondayAtMidday.toLocalDate())
        .dayIsEqualTo(Day.MON)
        .startTimeIsEqual(LocalTime.of(15, 0))
        .availableAdultsIsEqualTo(5)
        .availableGroupsIsEqualTo(4)
        .availableVideosIsEqualTo(1)
    }

    @Test
    fun `should be no available video slot on Monday afternoon when available video capacity met`() {
      availableSlots.add(availableSlot(Day.MON, 15, 5, 5, 3, effectiveDate = mondayAtMidday.toLocalDate()))

      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3), officialVisitId = 1, videoOnly = true))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3), officialVisitId = 1, videoOnly = true))
      bookedSlots.add(bookedSlot(mondayAtMidday.plusHours(3), officialVisitId = 1, videoOnly = true))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtMidday.toLocalDate(),
          mondayAtMidday.toLocalDate().plusDays(1),
          true,
        )

      freeSlots.size isEqualTo 0
    }
  }

  @Nested
  @DisplayName("Checking effective and expiry dates for available slots")
  inner class AvailableSlotsFromMondayMorningWhenExpiredOrBeforeEffectiveDate {
    private val mondayAtEightAm = LocalDate.of(2025, 11, 17).atTime(8, 0)
    private val availableSlots: MutableList<AvailableSlotEntity> = mutableListOf()
    private val bookedSlots: MutableList<VisitBookedEntity> = mutableListOf()

    @BeforeEach
    fun beforeEach() {
      availableSlotService = service(mondayAtEightAm)
      availableSlotRepository.stub { on { findAvailableVideoSlotsForPrison(MOORLAND, mondayAtEightAm.toLocalDate()) } doReturn availableSlots }
      visitBookedRepository.stub {
        on {
          findCurrentVisitsBookedBy(
            eq(MOORLAND),
            eq(mondayAtEightAm.toLocalDate()),
            any(),
          )
        } doReturn bookedSlots
      }
    }

    @Test
    fun `should leave 1 free slot for video on Monday morning and ignore an expired slot`() {
      availableSlots.add(availableSlot(Day.MON, 10, 5, 5, 3, effectiveDate = mondayAtEightAm.toLocalDate()))
      availableSlots.add(availableSlot(Day.MON, 11, 5, 5, 3, effectiveDate = mondayAtEightAm.toLocalDate(), expiryDate = mondayAtEightAm.toLocalDate().minusDays(1)))

      bookedSlots.add(bookedSlot(mondayAtEightAm.plusHours(2), officialVisitId = 1, videoOnly = true))
      bookedSlots.add(bookedSlot(mondayAtEightAm.plusHours(2), officialVisitId = 2, videoOnly = true))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtEightAm.toLocalDate(),
          mondayAtEightAm.toLocalDate().plusDays(1),
          true,
        )

      freeSlots
        .single()
        .dateIsEqualTo(mondayAtEightAm.toLocalDate())
        .dayIsEqualTo(Day.MON)
        .startTimeIsEqual(LocalTime.of(10, 0))
        .availableAdultsIsEqualTo(5)
        .availableGroupsIsEqualTo(3)
        .availableVideosIsEqualTo(1)
    }

    @Test
    fun `should leave 1 free slot for video and ignore the expired and not yet effective slots`() {
      // Available slot
      availableSlots.add(availableSlot(Day.MON, 10, 2, 2, 2, effectiveDate = mondayAtEightAm.toLocalDate()))

      // Not reached its effectiveDate
      availableSlots.add(availableSlot(Day.MON, 11, 1, 1, 1, effectiveDate = mondayAtEightAm.toLocalDate().plusDays(7)))

      // Both expired
      availableSlots.add(availableSlot(Day.MON, 11, 1, 1, 1, effectiveDate = mondayAtEightAm.toLocalDate().minusDays(3), expiryDate = mondayAtEightAm.toLocalDate().minusDays(1)))
      availableSlots.add(availableSlot(Day.MON, 12, 1, 1, 1, effectiveDate = mondayAtEightAm.toLocalDate().minusDays(3), expiryDate = mondayAtEightAm.toLocalDate().minusDays(1)))

      bookedSlots.add(bookedSlot(mondayAtEightAm.plusHours(2), officialVisitId = 1, videoOnly = true))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtEightAm.toLocalDate(),
          mondayAtEightAm.toLocalDate(),
          true,
        )

      freeSlots
        .single()
        .dateIsEqualTo(mondayAtEightAm.toLocalDate())
        .dayIsEqualTo(Day.MON)
        .startTimeIsEqual(LocalTime.of(10, 0))
        .availableAdultsIsEqualTo(2)
        .availableGroupsIsEqualTo(1)
        .availableVideosIsEqualTo(1)
    }

    @Test
    fun `should all be available slots if the expiry dates are in the future`() {
      availableSlots.add(availableSlot(Day.MON, 9, 1, 1, 1, effectiveDate = mondayAtEightAm.toLocalDate(), expiryDate = mondayAtEightAm.toLocalDate().plusDays(2)))
      availableSlots.add(availableSlot(Day.MON, 10, 1, 1, 1, effectiveDate = mondayAtEightAm.toLocalDate(), expiryDate = mondayAtEightAm.toLocalDate().plusDays(2)))
      availableSlots.add(availableSlot(Day.MON, 11, 1, 1, 1, effectiveDate = mondayAtEightAm.toLocalDate(), expiryDate = mondayAtEightAm.toLocalDate().plusDays(2)))

      val freeSlots =
        availableSlotService.getAvailableSlotsForPrison(
          MOORLAND,
          mondayAtEightAm.toLocalDate(),
          mondayAtEightAm.toLocalDate().plusDays(1),
          videoOnly = true,
        )

      freeSlots.size isEqualTo 3
    }
  }

  private fun service(dateTime: LocalDateTime) = AvailableSlotService(
    { dateTime },
    visitBookedRepository,
    availableSlotRepository,
    locationsService,
  )

  private fun availableSlot(
    day: Day,
    startHour: Int,
    maxAdults: Int = 1,
    maxGroups: Int = 1,
    maxVideo: Int = 0,
    effectiveDate: LocalDate = LocalDate.now(),
    expiryDate: LocalDate? = null,
  ) = AvailableSlotEntity(
    prisonVisitSlotId = startHour.toLong(),
    prisonTimeSlotId = startHour.toLong(),
    prisonCode = MOORLAND,
    displaySequence = -1,
    dayCode = day.toString(),
    dayDescription = day.toString(),
    startTime = LocalTime.of(startHour, 0),
    endTime = LocalTime.of(startHour, 59),
    effectiveDate = effectiveDate,
    expiryDate = expiryDate,
    dpsLocationId = UUID.randomUUID(),
    maxAdults = maxAdults,
    maxGroups = maxGroups,
    maxVideoSessions = maxVideo,
  )

  private fun bookedSlot(dateTime: LocalDateTime, officialVisitId: Long = -1, videoOnly: Boolean = false) = VisitBookedEntity(
    officialVisitId = officialVisitId,
    prisonCode = MOORLAND,
    dayCode = dateTime.dayOfWeek.toString().take(3).uppercase(),
    dayDescription = dateTime.dayOfWeek.toString(),
    prisonVisitSlotId = dateTime.toLocalTime().hour.toLong(),
    prisonTimeSlotId = dateTime.toLocalTime().hour.toLong(),
    visitDate = dateTime.toLocalDate(),
    startTime = dateTime.toLocalTime(),
    endTime = dateTime.toLocalTime(),
    visitStatusCode = "--",
    visitTypeCode = if (videoOnly) VisitType.VIDEO.toString() else VisitType.IN_PERSON.toString(),
    prisonerNumber = "--",
    contactId = -1,
    visitorTypeCode = "--",
    relationshipTypeCode = RelationshipType.OFFICIAL.toString(),
    relationshipCode = "--",
    firstName = "first name",
    lastName = "last name",
    dpsLocationId = UUID.randomUUID(),
  )

  private fun officialVisitLocations() = listOf(
    Location(
      id = UUID.randomUUID(),
      prisonId = MOORLAND,
      localName = "A name",
      code = "Code",
      pathHierarchy = "A-1-1-1",
      locationType = Location.LocationType.VISITS,
      permanentlyInactive = false,
      status = Location.Status.ACTIVE,
      level = 3,
      key = "A-1-1-1",
      active = true,
      locked = false,
      isResidential = false,
      leafLevel = true,
      topLevelId = UUID.randomUUID(),
      deactivatedByParent = false,
      lastModifiedBy = "XXX",
      lastModifiedDate = LocalDateTime.now().minusDays(1),
    ),
  )

  private fun AvailableSlot.dateIsEqualTo(expected: LocalDate) = also { visitDate isEqualTo expected }
  private fun AvailableSlot.dayIsEqualTo(expected: Day) = also { dayCode isEqualTo expected.toString() }
  private fun AvailableSlot.startTimeIsEqual(expected: LocalTime) = also { startTime isEqualTo expected }
  private fun AvailableSlot.availableAdultsIsEqualTo(expected: Int) = also { availableAdults isEqualTo expected }
  private fun AvailableSlot.availableGroupsIsEqualTo(expected: Int) = also { availableGroups isEqualTo expected }
  private fun AvailableSlot.availableVideosIsEqualTo(expected: Int) = also { availableVideoSessions isEqualTo expected }
}
