package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Sql("classpath:integration-test-data/creation/clean-visit-seed-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class GetOfficialVisitById : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonerVisitedRepository: PrisonerVisitedRepository

  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 123,
    prisonerContactId = 456,
    leadVisitor = true,
    assistedVisit = false,
  )

  private val nextMondayAt9 = CreateOfficialVisitRequest(
    prisonCode = MOORLAND_PRISONER.prison,
    prisonerNumber = MOORLAND_PRISONER.number,
    prisonVisitSlotId = 1,
    visitDate = next(DayOfWeek.MONDAY),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    visitTypeCode = VisitType.IN_PERSON,
    staffNotes = "private notes",
    prisonerNotes = "public notes",
    officialVisitors = listOf(officialVisitor),
  )

  @Test
  fun `should return error message  with  invalid official visit Id`() {
    webTestClient.getOfficialVisitsByInvalidId(9999L, MOORLAND_PRISONER.prison)
  }

  @Test
  fun `should create official visit with one social visitor and return official visit by id`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)
    val response = webTestClient.create(nextMondayAt9)
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
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
    }
  }

  private fun WebTestClient.create(request: CreateOfficialVisitRequest) = this
    .post()
    .uri("/official-visit/prison/${request.prisonCode}")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(CreateOfficialVisitResponse::class.java)
    .returnResult().responseBody!!

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
