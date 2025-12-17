package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitSummarySearchResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Sql("classpath:integration-test-data/creation/clean-visit-seed-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OfficialVisitSearchIntegrationTest : IntegrationTestBase() {

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

  private val nextMondayAt9 = CreateOfficialVisitRequest(
    prisonerNumber = MOORLAND_PRISONER.number,
    prisonVisitSlotId = 1,
    visitDate = next(DayOfWeek.MONDAY),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    visitTypeCode = VisitType.IN_PERSON,
    staffNotes = "private notes",
    prisonerNotes = "public notes",
    searchTypeCode = SearchLevelType.PAT,
    officialVisitors = listOf(officialVisitor),
  )

  private val nextWednesdayAt9 = nextMondayAt9.copy(prisonVisitSlotId = 4, visitDate = next(DayOfWeek.WEDNESDAY), startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))

  @Test
  fun `should find official visits by criteria over multiple pages`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)

    testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)
    testAPIClient.createOfficialVisit(nextWednesdayAt9, MOORLAND_PRISON_USER)

    val searchRequest = OfficialVisitSummarySearchRequest(
      startDate = next(DayOfWeek.MONDAY),
      endDate = next(DayOfWeek.WEDNESDAY),
      visitTypes = emptyList(),
      visitStatuses = emptyList(),
      prisonerNumbers = listOf(MOORLAND_PRISONER.number),
      locationIds = emptyList(),
    )

    val pageOne = webTestClient.search(searchRequest, MOORLAND_PRISON_USER, 0, 1)

    with(pageOne) {
      content.single().visitSlotId isEqualTo 1
      page.size isEqualTo 1
      page.number isEqualTo 0
      page.totalElements isEqualTo 2
      page.totalPages isEqualTo 2
    }

    val pageTwo = webTestClient.search(searchRequest, MOORLAND_PRISON_USER, 1, 1)

    with(pageTwo) {
      content.single().visitSlotId isEqualTo 4
      page.size isEqualTo 1
      page.number isEqualTo 1
      page.totalElements isEqualTo 2
      page.totalPages isEqualTo 2
    }
  }

  @Test
  fun `should find official all visits by criteria on one page`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)

    testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)
    testAPIClient.createOfficialVisit(nextWednesdayAt9, MOORLAND_PRISON_USER)

    val searchRequest = OfficialVisitSummarySearchRequest(
      startDate = next(DayOfWeek.MONDAY),
      endDate = next(DayOfWeek.WEDNESDAY),
      visitTypes = emptyList(),
      visitStatuses = emptyList(),
      prisonerNumbers = listOf(MOORLAND_PRISONER.number),
      locationIds = emptyList(),
    )

    val onePageOnly = webTestClient.search(searchRequest, MOORLAND_PRISON_USER, 0, 2)

    with(onePageOnly) {
      assertThat(content).extracting("visitSlotId").containsExactlyInAnyOrder(1L, 4L)
      page.size isEqualTo 2
      page.number isEqualTo 0
      page.totalElements isEqualTo 2
      page.totalPages isEqualTo 1
    }
  }

  fun WebTestClient.search(request: OfficialVisitSummarySearchRequest, prisonUser: PrisonUser, page: Int = 0, size: Int = 1) = webTestClient
    .post()
    .uri("/official-visit/prison/${prisonUser.activeCaseLoadId}/find-by-criteria?page=$page&size=$size")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(SearchResponse::class.java)
    .returnResult().responseBody!!

  data class SearchResponse(
    val content: List<OfficialVisitSummarySearchResponse>,
    val page: PagedModel.PageMetadata,
  )
}
