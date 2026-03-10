package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot
import java.time.DayOfWeek.FRIDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class AvailableSlotsIntegrationTest : IntegrationTestBase() {

  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 123,
    prisonerContactId = 456,
    leadVisitor = true,
    assistedVisit = false,
    assistedNotes = "visitor notes",
    visitorEquipment = VisitorEquipment("Bringing secure laptop"),
  )

  private final val nextFridaySlot9At11 = createOfficialVisitRequest(Moorland.FRIDAY_11_TO_12_VISIT_SLOT.copy(date = today().next(FRIDAY)), listOf(officialVisitor))

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()

    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(MOORLAND, fakeOfficialVisitLocations())

    // Stub a known contact
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = officialVisitor.contactId!!,
          prisonerContactId = officialVisitor.prisonerContactId!!,
        ),
      ),
    )
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should perform basic GET with no visits and ignoring expired and inactive slots`() {
    val nextFriday = today().next(FRIDAY)

    // An expired slot from 2023 exists in base data for Fridays at 9:10 until 10:10
    // A future-dated slot for 2040 exists in base data for Fridays at 9:15 until 10:15

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
  fun `should perform GET with slot 9 on Friday at 11 no longer available`() {
    val nextFriday = today().next(FRIDAY)

    // An expired slot from 2023 exists in base data for Fridays at 9:10 until 10:10
    // A future-dated slot for 2040 exists in base data for Fridays at 9:15 until 10:15

    val responseWithSlot9At11Fri = webTestClient.availableSlots(prisonCode = MOORLAND, fromDate = nextFriday, toDate = nextFriday)

    // Slot 9 on the Friday at 11 should be in the response
    responseWithSlot9At11Fri containsExactlyInAnyOrder listOf(
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

    // Fill up capacity for slot 9 at 11 on Friday
    testAPIClient.createOfficialVisit(request = nextFridaySlot9At11, MOORLAND_PRISON_USER)

    val responseWithoutSlot9At11Fri = webTestClient.availableSlots(prisonCode = MOORLAND, fromDate = nextFriday, toDate = nextFriday)

    // Slot 9 on the Friday at 11 should not be in the response
    responseWithoutSlot9At11Fri containsExactlyInAnyOrder listOf(
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
    )
  }

  private fun WebTestClient.availableSlots(prisonCode: String, fromDate: LocalDate, toDate: LocalDate) = this
    .get()
    .uri("/available-slots/$prisonCode?fromDate=$fromDate&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList<AvailableSlot>()
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
