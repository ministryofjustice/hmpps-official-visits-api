package uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toTimeSlotModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toVisitSlotModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummaryItem
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class AdminServiceTest {
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val locationService: LocationsService = mock()

    private val dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")
  private val adminService =
    AdminService(prisonTimeSlotRepository, prisonVisitSlotRepository, prisonerSearchClient, locationService)

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `should return summary of active time slots and associated visit slots for the prison code`() {
    val timeSlotEntity = PrisonTimeSlotEntity(
      prisonTimeSlotId = 1L,
      prisonCode = MOORLAND_PRISONER.prison,
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusDays(365),
      createdBy = "Test",
      createdTime = LocalDateTime.now(),
    )


      val visitSlotEntity = PrisonVisitSlotEntity(
      prisonVisitSlotId = 1L,
      prisonTimeSlotId = 1L,
      dpsLocationId = dpsLocationId,
      maxAdults = 10,
      createdBy = "Test",
      createdTime = LocalDateTime.now(),
    )

    whenever(prisonTimeSlotRepository.findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)).thenReturn(
      listOf(
        timeSlotEntity,
      ),
    )
    whenever(prisonVisitSlotRepository.findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))).thenReturn(
      listOf(visitSlotEntity),
    )
    whenever(prisonerSearchClient.findPrisonersBySearchTerm(MOORLAND_PRISONER.prison, searchTerm = "")).thenReturn(
      listOf(
        prisonerSearchPrisoner(
          prisonerNumber = MOORLAND_PRISONER.number,
          prisonCode = MOORLAND_PRISONER.prison,
          bookingId = MOORLAND_PRISONER.bookingId,
        ),
      ),
    )

      whenever(locationService.getOfficialVisitLocationsAtPrison(MOORLAND_PRISONER.prison))
          .thenReturn(officialVisitLocations(dpsLocationId))

    val summary = adminService.getAllPrisonTimeSlotsAndAssociatedVisitSlots(MOORLAND_PRISONER.prison, true)

    assertThat(summary).isEqualTo(
      TimeSlotSummary(
        prisonCode = MOORLAND_PRISONER.prison,
        prisonName = "A prison",
        timeSlots =
        listOf(
          TimeSlotSummaryItem(
            timeSlotEntity.toTimeSlotModel(),
            listOf(visitSlotEntity.toVisitSlotModel(MOORLAND_PRISONER.prison).copy( locationDescription = location(this@AdminServiceTest.dpsLocationId).localName,
                locationCapacity = location(this@AdminServiceTest.dpsLocationId).capacity?.maxCapacity)),
          )
        ),
      )
    )

    verify(prisonTimeSlotRepository).findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)
    verify(prisonVisitSlotRepository).findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))
    verifyNoMoreInteractions(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

    private fun officialVisitLocations(dpsLocationId: UUID) = listOf(
        location(dpsLocationId),
    )

    private fun location(dpsLocationId: UUID): Location = Location(
        id = dpsLocationId,
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
    )
}
