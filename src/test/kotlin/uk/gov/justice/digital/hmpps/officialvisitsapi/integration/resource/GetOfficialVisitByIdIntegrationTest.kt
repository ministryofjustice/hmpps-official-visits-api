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
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class GetOfficialVisitByIdIntegrationTest : IntegrationTestBase() {

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
  fun `should return an error when the official visit ID does not exist`() {
    webTestClient.getOfficialVisitsByInvalidId(9999L, MOORLAND_PRISONER.prison)
  }

  @Test
  fun `should create official visit with one social visitor and return official visit by id`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)
    personalRelationshipsApi().stubReferenceGroup()

    val response = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    val prisonerVisit = prisonerVisitedRepository.findByOfficialVisitId(response.officialVisitId)
    prisonerVisit!!.officialVisit.officialVisitId isEqualTo response.officialVisitId

    val officialVisitDetails = webTestClient.getOfficialVisitsByIdAndPrisonCode(response.officialVisitId, MOORLAND_PRISONER.prison)

    assertThat(officialVisitDetails).isNotNull
    with(officialVisitDetails) {
      officialVisitId isEqualTo response.officialVisitId
      prisonCode isEqualTo MOORLAND_PRISONER.prison
      dpsLocationId isEqualTo UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")
      visitTypeCode isEqualTo VisitType.IN_PERSON
      staffNotes isEqualTo "private notes"
      prisonerNotes isEqualTo "public notes"
      visitDate isEqualTo visitDateInTheFuture
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
    }
  }

  private fun WebTestClient.getOfficialVisitsByInvalidId(officialVisitId: Long, prisonCode: String) = this
    .get()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN"))).exchange()
    .expectStatus().is4xxClientError
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody().jsonPath("$.userMessage").isEqualTo("Official visit with id $officialVisitId and prison code $prisonCode not found")

  private fun WebTestClient.getOfficialVisitsByIdAndPrisonCode(officialVisitId: Long, prisonCode: String) = this
    .get()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(OfficialVisitDetails::class.java)
    .returnResult().responseBody!!
}
