package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumingThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot
import java.time.DayOfWeek.FRIDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class AvailableSlotsIntegrationTest : IntegrationTestBase() {
  private val today = LocalDate.now()

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(MOORLAND, fakeOfficialVisitLocations())
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should perform basic GET with no visits`() {
    val nextFriday = today.next(FRIDAY)

    val response = webTestClient.availableSlots(prisonCode = MOORLAND, fromDate = nextFriday, toDate = nextFriday)

    response containsExactlyInAnyOrder listOf(
      AvailableSlot(
        visitSlotId = 7,
        timeSlotId = 7,
        prisonCode = "MDI",
        dayCode = "FRI",
        dayDescription = "Friday",
        visitDate = nextFriday,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
        availableVideoSessions = 4,
        availableAdults = 10,
        availableGroups = 5,
        locationDescription = "Location description A",
      ),
      AvailableSlot(
        visitSlotId = 8,
        timeSlotId = 8,
        prisonCode = "MDI",
        dayCode = "FRI",
        dayDescription = "Friday",
        visitDate = nextFriday,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        dpsLocationId = UUID.fromString("50b61cbe-e42b-4a77-a00e-709b0421b8ed"),
        availableVideoSessions = 4,
        availableAdults = 10,
        availableGroups = 5,
        locationDescription = "Location description B",
      ),
      AvailableSlot(
        visitSlotId = 9,
        timeSlotId = 9,
        prisonCode = "MDI",
        dayCode = "FRI",
        dayDescription = "Friday",
        visitDate = nextFriday,
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(12, 0),
        dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
        availableVideoSessions = 1,
        availableAdults = 1,
        availableGroups = 1,
        locationDescription = "Location description A",
      ),
    )
  }

  @Test
  fun `should perform GET with existing Friday visits for slot 7 and 9`() {
    assumingThat(today.dayOfWeek == FRIDAY && LocalTime.now() < LocalTime.of(9, 0)) {
      val response = webTestClient.availableSlots(prisonCode = MOORLAND, fromDate = today, toDate = today)

      // Slot 9 is fully booked so should not be in the response
      response containsExactlyInAnyOrder listOf(
        AvailableSlot(
          visitSlotId = 7,
          timeSlotId = 7,
          prisonCode = "MDI",
          dayCode = "FRI",
          dayDescription = "Friday",
          visitDate = today,
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(10, 0),
          dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
          availableVideoSessions = 0,
          availableAdults = 9,
          availableGroups = 4,
          locationDescription = "Location description A",
        ),
        AvailableSlot(
          visitSlotId = 8,
          timeSlotId = 8,
          prisonCode = "MDI",
          dayCode = "FRI",
          dayDescription = "Friday",
          visitDate = today,
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          dpsLocationId = UUID.fromString("50b61cbe-e42b-4a77-a00e-709b0421b8ed"),
          availableVideoSessions = 0,
          availableAdults = 10,
          availableGroups = 5,
          locationDescription = "Location description B",
        ),
      )
    }
  }

  private fun WebTestClient.availableSlots(prisonCode: String, fromDate: LocalDate, toDate: LocalDate) = this
    .get()
    .uri("/available-slots/$prisonCode?fromDate=$fromDate&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(AvailableSlot::class.java)
    .returnResult().responseBody!!

  private fun fakeOfficialVisitLocations() = listOf(
    Location(
      id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      prisonId = MOORLAND,
      localName = "Location description A",
      code = "LOC-A",
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
    Location(
      id = UUID.fromString("50b61cbe-e42b-4a77-a00e-709b0421b8ed"),
      prisonId = MOORLAND,
      localName = "Location description B",
      code = "LOC-B",
      pathHierarchy = "B-1-1-1",
      locationType = Location.LocationType.VISITS,
      permanentlyInactive = false,
      status = Location.Status.ACTIVE,
      level = 3,
      key = "B-1-1-1",
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
}
