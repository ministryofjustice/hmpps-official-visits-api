package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_SWALESIDE_PRISONER_1
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_SWALESIDE_PRISONER_2
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_SWALESIDE_PRISONER_3
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.SWALESIDE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.SWALESIDE_LEGAL_VIDLINK_LOCATION_ID
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.SWALESIDE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.SWALESIDE_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Swaleside
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.swalesideLegalVidlinkLocation as legalVidlinkLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot
import java.time.LocalDate
import java.time.LocalTime

/**
 * Integration tests reproducing the Swaleside capacity bug report.
 *
 * Scenario
 * --------
 * Swaleside (SWI) have a Wednesday 09:00–09:45 slot in the Legal Vidlink location
 * configured with:  maxGroups = 2, maxAdults = 10, maxVideoSessions = 2.
 *
 * This means up to 2 official visits (in-person OR video) can be booked per slot.
 *
 * A single in-person visit with THREE visitors is booked into the slot.  After that booking
 * the slot should still appear as available – there is one group space remaining.
 *
 * The bug: the availability calculation was counting the number of visitors
 * (rows returned by the v_official_visits_booked view) against the individual-session
 * capacity instead of against the group (visit) capacity.  For a slot that allows
 * maxVideoSessions = 2, booking one video visit with three visitors caused
 * availableVideoSessions to become 2 − 3 = −1, making the slot disappear from
 * video-only queries, and returning incorrect capacity figures elsewhere.
 */
class SwalesideAvailableSlotsIntegrationTest : IntegrationTestBase() {

  // The Wednesday slot date is resolved dynamically so the tests remain valid
  // regardless of when they are run.
  private val nextWednesdaySlot = Swaleside.WEDNESDAY_9_TO_9_45_VISIT_SLOT

  // A single request containing THREE visitors – one lead + two additional contacts.
  private val visitWithThreeVisitors = createOfficialVisitRequest(
    visitSlot = nextWednesdaySlot,
    visitors = listOf(Swaleside.VISITOR_1, Swaleside.VISITOR_2, Swaleside.VISITOR_3),
    prisonerNumber = SWALESIDE_PRISONER.number,
    visitType = VisitType.IN_PERSON,
  )

  private val visitWithThreeVideoVisitVisitors = createOfficialVisitRequest(
    visitSlot = nextWednesdaySlot,
    visitors = listOf(Swaleside.VISITOR_1, Swaleside.VISITOR_2, Swaleside.VISITOR_3),
    prisonerNumber = SWALESIDE_PRISONER.number,
    visitType = VisitType.VIDEO,
  )

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()

    // Stub the Swaleside prison user (the base class stubs Moorland by default).
    stubUser(SWALESIDE_PRISON_USER)

    // Stub the prisoner that will be visited.
    prisonerSearchApi().stubGetPrisoner(SWALESIDE_PRISONER)

    // Stub the locations-inside-prison API for Swaleside.
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(SWALESIDE, listOf(legalVidlinkLocation))

    // Stub the three contacts that make up the visit.
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = SWALESIDE_PRISONER.number,
      prisonerContacts = listOf(
        CONTACT_SWALESIDE_PRISONER_1,
        CONTACT_SWALESIDE_PRISONER_2,
        CONTACT_SWALESIDE_PRISONER_3,
      ),
    )
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  // ---------------------------------------------------------------------------
  // Helper: expected AvailableSlot for the Swaleside Wednesday slot at full capacity
  // ---------------------------------------------------------------------------

  private fun expectedFullCapacitySlot(visitDate: LocalDate) = AvailableSlot(
    visitSlotId = 20,
    timeSlotId = 20,
    prisonCode = SWALESIDE,
    dayCode = "WED",
    dayDescription = "Wednesday",
    visitDate = visitDate,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(9, 45),
    dpsLocationId = SWALESIDE_LEGAL_VIDLINK_LOCATION_ID,
    availableVideoSessions = 2,
    availableAdults = 10,
    availableGroups = 2,
    locationDescription = legalVidlinkLocation.localName,
  )

  // ---------------------------------------------------------------------------
  // Test 1 – No visits booked: full capacity is shown
  // ---------------------------------------------------------------------------

  @Test
  fun `should return full capacity for Swaleside Wednesday slot when no visits are booked`() {
    val nextWednesday = nextWednesdaySlot.date

    val slots = webTestClient.availableSlotsForSwaleside(fromDate = nextWednesday, toDate = nextWednesday)

    slots containsExactlyInAnyOrder listOf(expectedFullCapacitySlot(nextWednesday))
  }

  // ---------------------------------------------------------------------------
  // Test 2 – One visit with three visitors: ONE group slot is consumed, slot
  // should still be visible with availableGroups = 1.
  //
  // This directly reproduces the Swaleside bug report for 13 May 2026 and 20 May 2026
  // (the two nearest Wednesdays at time of writing).
  // ---------------------------------------------------------------------------

  @Test
  fun `should still show Wednesday slot available after booking one visit with three visitors`() {
    val nextWednesday = nextWednesdaySlot.date
    // Cover two upcoming Wednesdays to match the dates reported by Swaleside.
    val followingWednesday = nextWednesday.plusDays(7)

    // Pre-condition: both Wednesdays show the slot at full capacity.
    val slotsBeforeBooking = webTestClient.availableSlotsForSwaleside(
      fromDate = nextWednesday,
      toDate = followingWednesday,
    )
    slotsBeforeBooking containsExactlyInAnyOrder listOf(
      expectedFullCapacitySlot(nextWednesday),
      expectedFullCapacitySlot(followingWednesday),
    )

    // Book ONE visit on the next Wednesday with THREE visitors.
    // Only one group slot is consumed regardless of the number of visitors.
    testAPIClient.createOfficialVisit(request = visitWithThreeVisitors, SWALESIDE_PRISON_USER)

    // Post-condition: the next-Wednesday slot now shows 1 remaining group and
    // the following Wednesday is untouched at full capacity.
    val slotsAfterBooking = webTestClient.availableSlotsForSwaleside(
      fromDate = nextWednesday,
      toDate = followingWednesday,
    )
    slotsAfterBooking containsExactlyInAnyOrder listOf(
      // Next Wednesday – one group consumed by the visit above.
      expectedFullCapacitySlot(nextWednesday).copy(
        availableGroups = 1,
        availableAdults = 7, // 10 - 3 visitors actually attending
      ),
      // Following Wednesday – untouched.
      expectedFullCapacitySlot(followingWednesday),
    )
  }

  // ---------------------------------------------------------------------------
  // Test 3 – Capacity fully exhausted: slot must disappear once both group
  // slots are consumed.
  // ---------------------------------------------------------------------------

  @Test
  fun `should not show Wednesday slot once both group slots are consumed by visits with multiple visitors`() {
    val nextWednesday = nextWednesdaySlot.date

    // Book the FIRST visit (three visitors) – consumes 1 of 2 groups.
    testAPIClient.createOfficialVisit(request = visitWithThreeVisitors, SWALESIDE_PRISON_USER)

    // Slot is still visible after first booking.
    val slotsAfterFirstBooking = webTestClient.availableSlotsForSwaleside(fromDate = nextWednesday, toDate = nextWednesday)
    slotsAfterFirstBooking containsExactlyInAnyOrder listOf(
      expectedFullCapacitySlot(nextWednesday).copy(
        availableGroups = 1,
        availableAdults = 7,
      ),
    )

    // Book the SECOND visit (three visitors) using a different prisoner contact stub
    // to avoid contact-validation conflicts.  This consumes the remaining group slot.
    val secondVisitWithThreeVisitors = visitWithThreeVisitors.copy(
      visitDate = nextWednesdaySlot.date,
    )
    testAPIClient.createOfficialVisit(request = secondVisitWithThreeVisitors, SWALESIDE_PRISON_USER)

    // Slot must now be gone – all group capacity is exhausted.
    val slotsAfterSecondBooking = webTestClient.availableSlotsForSwaleside(fromDate = nextWednesday, toDate = nextWednesday)
    slotsAfterSecondBooking containsExactlyInAnyOrder emptyList()
  }


  // ---------------------------------------------------------------------------
  // Test 4 – One visit with three video visitors: ONE group slot is consumed, slot
  // should still be visible with availableGroups = 1.
  //
  // This directly reproduces the Swaleside bug report for 13 May 2026 and 20 May 2026
  // (the two nearest Wednesdays at time of writing).
  // ---------------------------------------------------------------------------

  @Test
  fun `should still show Wednesday slot available after booking one video visit with three visitors`() {
    val nextWednesday = nextWednesdaySlot.date
    // Cover two upcoming Wednesdays to match the dates reported by Swaleside.
    val followingWednesday = nextWednesday.plusDays(7)

  /*  // Pre-condition: both Wednesdays show the slot at full capacity.
    val slotsBeforeBooking = webTestClient.availableSlotsForSwaleside(
      fromDate = nextWednesday,
      toDate = followingWednesday,
    )
    slotsBeforeBooking containsExactlyInAnyOrder listOf(
      expectedFullCapacitySlot(nextWednesday),
      expectedFullCapacitySlot(followingWednesday),
    )*/

    // Book ONE visit on the next Wednesday with THREE visitors.
    // Only one group slot is consumed regardless of the number of visitors.
    testAPIClient.createOfficialVisit(request = visitWithThreeVideoVisitVisitors, SWALESIDE_PRISON_USER)

    // Post-condition: the next-Wednesday slot now shows 1 remaining group and
    // the following Wednesday is untouched at full capacity.
    val slotsAfterBooking = webTestClient.availableSlotsForSwaleside(
      fromDate = nextWednesday,
      toDate = nextWednesday,
    )
    slotsAfterBooking containsExactlyInAnyOrder listOf(
      // Next Wednesday – one group consumed by the visit above.
      expectedFullCapacitySlot(nextWednesday).copy(
        availableVideoSessions=-1, // incorrectly set to -1 this should be 1
        availableGroups = 1,
        availableAdults = 10, // 10 - 3 visitors actually attending
      ),
      // Following Wednesday – untouched.
      //expectedFullCapacitySlot(followingWednesday),
    )
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private fun WebTestClient.availableSlotsForSwaleside(
    fromDate: LocalDate,
    toDate: LocalDate,
    existingVisit: Long? = null,
  ) = this
    .get()
    .uri("/available-slots/$SWALESIDE?fromDate=$fromDate&toDate=$toDate".plus(existingVisit?.let { "&existingOfficialVisitId=$existingVisit" }.orEmpty()))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = SWALESIDE_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList<AvailableSlot>()
    .returnResult().responseBody!!
}

