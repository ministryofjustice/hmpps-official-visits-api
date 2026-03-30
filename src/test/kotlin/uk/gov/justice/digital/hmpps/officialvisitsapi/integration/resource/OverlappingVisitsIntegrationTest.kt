package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.location
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OverlappingVisitsCriteriaRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OverlappingVisitsResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import kotlin.properties.Delegates

class OverlappingVisitsIntegrationTest : IntegrationTestBase() {
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

  private val nextMondayAt9 = createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor))
  private val nextWednesdayAt9 = createOfficialVisitRequest(Moorland.WEDNESDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor))
  private val nextFridayAt11 = createOfficialVisitRequest(Moorland.FRIDAY_11_TO_12_VISIT_SLOT, listOf(officialVisitor))
  private var nextMondayAt9VisitId by Delegates.notNull<Long>()

  @BeforeEach
  @Transactional
  fun beforeEach() {
    clearAllVisitData()

    // Stub a known contact
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 123,
          prisonerContactId = 456,
        ),
      ),
    )

    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(
      prisonCode = MOORLAND,
      locations = listOf(
        location(
          prisonCode = MOORLAND,
          locationKeySuffix = "1-1",
          localName = "Visit place",
          id = moorlandLocation.id,
        ),
      ),
    )

    // We need more that one visit for the contact under test to ensure queries are stable.
    nextMondayAt9VisitId = testAPIClient.createOfficialVisit(nextMondayAt9).officialVisitId
    testAPIClient.createOfficialVisit(nextWednesdayAt9)
  }

  @Test
  fun `should clash with existing visit when exactly overlaps`() {
    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = nextMondayAt9.visitDate,
      startTime = nextMondayAt9.startTime,
      endTime = nextMondayAt9.endTime,
      contactIds = listOf(officialVisitor.contactId!!),
    )

    with(webTestClient.check(request)) {
      prisonerNumber isEqualTo request.prisonerNumber
      overlappingPrisonerVisits containsExactlyInAnyOrder listOf(nextMondayAt9VisitId)
      contacts.single().contactId isEqualTo officialVisitor.contactId
      contacts.single().overlappingContactVisits containsExactlyInAnyOrder listOf(nextMondayAt9VisitId)
    }
  }

  @Test
  fun `should clash with existing visit when earlier but still overlaps`() {
    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = nextMondayAt9.visitDate,
      startTime = nextMondayAt9.startTime!!.minusMinutes(1),
      endTime = nextMondayAt9.endTime!!.minusMinutes(1),
      contactIds = listOf(officialVisitor.contactId!!),
    )

    with(webTestClient.check(request)) {
      prisonerNumber isEqualTo request.prisonerNumber
      overlappingPrisonerVisits containsExactlyInAnyOrder listOf(nextMondayAt9VisitId)
      contacts.single().contactId isEqualTo officialVisitor.contactId
      contacts.single().overlappingContactVisits containsExactlyInAnyOrder listOf(nextMondayAt9VisitId)
    }
  }

  @Test
  fun `should clash with existing visit when later but still overlaps`() {
    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = nextMondayAt9.visitDate,
      startTime = nextMondayAt9.startTime!!.plusMinutes(1),
      endTime = nextMondayAt9.endTime!!.plusMinutes(1),
      contactIds = listOf(officialVisitor.contactId!!),
    )

    with(webTestClient.check(request)) {
      prisonerNumber isEqualTo request.prisonerNumber
      overlappingPrisonerVisits containsExactlyInAnyOrder listOf(nextMondayAt9VisitId)
      contacts.single().contactId isEqualTo officialVisitor.contactId
      contacts.single().overlappingContactVisits containsExactlyInAnyOrder listOf(nextMondayAt9VisitId)
    }
  }

  @Test
  fun `should not clash with existing visit when exising visit is ignored`() {
    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = nextMondayAt9.visitDate,
      startTime = nextMondayAt9.startTime,
      endTime = nextMondayAt9.endTime,
      contactIds = listOf(officialVisitor.contactId!!),
      existingOfficialVisitId = nextMondayAt9VisitId,
    )

    with(webTestClient.check(request)) {
      prisonerNumber isEqualTo request.prisonerNumber
      overlappingPrisonerVisits hasSize 0
      contacts.single().contactId isEqualTo officialVisitor.contactId
      contacts.single().overlappingContactVisits hasSize 0
    }
  }

  @Test
  fun `should not clash with existing visit when exising visit is at a different time`() {
    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = nextFridayAt11.visitDate,
      startTime = nextFridayAt11.startTime,
      endTime = nextFridayAt11.endTime,
      contactIds = listOf(officialVisitor.contactId!!),
    )

    with(webTestClient.check(request)) {
      prisonerNumber isEqualTo request.prisonerNumber
      overlappingPrisonerVisits hasSize 0
      contacts.single().contactId isEqualTo officialVisitor.contactId
      contacts.single().overlappingContactVisits hasSize 0
    }
  }

  @Test
  fun `should accept empty contactIds for prisoner-only overlap check`() {
    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = nextFridayAt11.visitDate,
      startTime = nextFridayAt11.startTime,
      endTime = nextFridayAt11.endTime,
      contactIds = emptyList(),
    )

    with(webTestClient.check(request)) {
      prisonerNumber isEqualTo request.prisonerNumber
      overlappingPrisonerVisits hasSize 0
      contacts hasSize 0
    }
  }

  private fun WebTestClient.check(request: OverlappingVisitsCriteriaRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .post()
    .uri("/official-visit/prison/${prisonUser.activeCaseLoadId}/overlapping")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<OverlappingVisitsResponse>()
    .returnResult().responseBody!!
}
