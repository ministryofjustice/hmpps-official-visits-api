package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class ReconciliationIntegrationTest : IntegrationTestBase() {

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
  fun `Get one by ID - should return an official visit for reconciliation`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)
    personalRelationshipsApi().stubReferenceGroup()
    val visit = webTestClient.create(nextMondayAt9)

    val response = webTestClient.getOfficialVisitById(visit.officialVisitId)

    assertThat(response).isNotNull
    with(response) {
      officialVisitId isEqualTo visit.officialVisitId
      prisonCode isEqualTo MOORLAND_PRISONER.prison
      dpsLocationId isEqualTo UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")
      visitDate isEqualTo nextMondayAt9.visitDate
      startTime isEqualTo nextMondayAt9.startTime
      endTime isEqualTo nextMondayAt9.endTime
      visitors.size isEqualTo 1
      with(visitors.first()) {
        contactId isEqualTo nextMondayAt9.officialVisitors.first().contactId
        relationshipCode isEqualTo nextMondayAt9.officialVisitors.first().relationshipCode
      }
    }
  }

  @Test
  fun `Get one by ID - should return an error for an invalid ID`() {
    webTestClient.getOfficialVisitByInvalidId()
  }

  @Test
  fun `Get all IDs - no results`() {
    webTestClient.getOfficialVisitIdsEmpty()
  }

  @Test
  fun `Get all IDS - with results across two pages`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)
    personalRelationshipsApi().stubReferenceGroup()

    webTestClient.create(nextMondayAt9)
    webTestClient.create(nextFridayAt11)

    webTestClient.getOfficialVisitIdsPaged()
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

  private fun WebTestClient.getOfficialVisitById(officialVisitId: Long) = this
    .get()
    .uri("/reconcile/official-visit/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(SyncOfficialVisit::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.getOfficialVisitByInvalidId() = this
    .get()
    .uri("/reconcile/official-visit/id/999")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .exchange()
    .expectStatus().is4xxClientError
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody().jsonPath("$.userMessage").isEqualTo("Official visit with id 999 not found")

  private fun WebTestClient.getOfficialVisitIdsEmpty() = this
    .get()
    .uri("/reconcile/official-visits/identifiers?currentTerm=true&page=0&size=1")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody()
    .consumeWith(System.out::println)
    .jsonPath("$.content.length()").isEqualTo(0)
    .jsonPath("$.page.totalElements").isEqualTo(0)

  private fun WebTestClient.getOfficialVisitIdsPaged() = this
    .get()
    .uri("/reconcile/official-visits/identifiers?currentTerm=true&page=0&size=1")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody()
    .consumeWith(System.out::println)
    .jsonPath("$.content.length()").isEqualTo(1)
    .jsonPath("$.page.totalElements").isEqualTo(2)
}
