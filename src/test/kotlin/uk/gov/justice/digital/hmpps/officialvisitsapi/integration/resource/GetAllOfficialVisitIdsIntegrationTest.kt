package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class GetAllOfficialVisitIdsIntegrationTest : IntegrationTestBase() {

  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 123,
    prisonerContactId = 456,
    leadVisitor = true,
    assistedVisit = false,
  )

  private final val visitDateInTheFuture = today().next(DayOfWeek.MONDAY)

  private val nextMondayAt9 = CreateOfficialVisitRequest(
    prisonerNumber = MOORLAND_PRISONER.number,
    prisonVisitSlotId = 1,
    visitDate = visitDateInTheFuture,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    visitTypeCode = VisitType.IN_PERSON,
    staffNotes = "private notes",
    prisonerNotes = "public notes",
    officialVisitors = listOf(officialVisitor),
  )

  private final val nextFridayAt11 = nextMondayAt9.copy(
    visitDate = visitDateInTheFuture.next(DayOfWeek.FRIDAY),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(12, 0),
    prisonVisitSlotId = 9,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
  )

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should return zero official visit ids`() {
    webTestClient.getOfficialVisitsIdsNoResult()
  }

  @Test
  fun `Should return all official visit Ids`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)
    personalRelationshipsApi().stubReferenceGroup()

    // Create two visits
    webTestClient.create(nextMondayAt9)
    webTestClient.create(nextFridayAt11)

    // Get all IDs and check
    webTestClient.getOfficialVisitsIds()
  }

  private fun WebTestClient.create(request: CreateOfficialVisitRequest) = this
    .post()
    .uri("/official-visit/prison/${MOORLAND_PRISON_USER.activeCaseLoadId}")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(CreateOfficialVisitResponse::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.getOfficialVisitsIdsNoResult() = this
    .get()
    .uri("/reconcile/official-visits/identifiers?currentTerm=false&page=0&size=10")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody()
    .jsonPath("$.content").isArray
    .jsonPath("$.page.totalElements").isEqualTo(0)

  private fun WebTestClient.getOfficialVisitsIds() = this
    .get()
    .uri("/reconcile/official-visits/identifiers?currentTerm=true&page=0&size=1")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody()
    .jsonPath("$.content").isArray
    .jsonPath("$.content[0].officialVisitId").isNumber
    .jsonPath("$.page.totalElements").isEqualTo(2)
    .jsonPath("$.page.totalPages").isEqualTo(2)
}
