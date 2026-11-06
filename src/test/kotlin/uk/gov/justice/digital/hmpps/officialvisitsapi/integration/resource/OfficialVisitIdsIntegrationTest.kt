package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation2
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import java.time.LocalDate

class OfficialVisitIdsIntegrationTest : IntegrationTestBase() {

  private val mondayInPerson = createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(Moorland.VISITOR))
  private val wednesdayVideo = createOfficialVisitRequest(Moorland.WEDNESDAY_9_TO_10_VISIT_SLOT, listOf(Moorland.VISITOR), visitType = VisitType.VIDEO)
  private val fridayInPerson = createOfficialVisitRequest(Moorland.FRIDAY_11_TO_12_VISIT_SLOT, listOf(Moorland.VISITOR))

  private var mondayId = 0L
  private var wednesdayId = 0L
  private var fridayId = 0L

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()

    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(MOORLAND, listOf(moorlandLocation, moorlandLocation2))
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = Moorland.VISITOR.contactId!!,
          prisonerContactId = Moorland.VISITOR.prisonerContactId!!,
        ),
      ),
    )

    mondayId = testAPIClient.createOfficialVisit(mondayInPerson).officialVisitId
    wednesdayId = testAPIClient.createOfficialVisit(wednesdayVideo).officialVisitId
    fridayId = testAPIClient.createOfficialVisit(fridayInPerson).officialVisitId

    testAPIClient.cancel(
      officialVisitId = fridayId,
      request = OfficialVisitCancellationRequest(cancellationReason = VisitCompletionType.VISITOR_CANCELLED, cancellationNotes = "cancelled"),
    )
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should return all IDs ordered by ID when only the date range is supplied`() {
    val response = getIds()

    response.content isEqualTo listOf(mondayId, wednesdayId, fridayId)
    response.page.totalElements isEqualTo 3
  }

  @Test
  fun `should page through IDs`() {
    val firstPage = getIds("page=0&size=2")
    val secondPage = getIds("page=1&size=2")

    firstPage.content isEqualTo listOf(mondayId, wednesdayId)
    secondPage.content isEqualTo listOf(fridayId)
    with(firstPage.page) {
      size isEqualTo 2
      number isEqualTo 0
      totalElements isEqualTo 3
      totalPages isEqualTo 2
    }
  }

  @Test
  fun `should apply sort parameter`() {
    getIds("sort=officialVisitId,desc").content isEqualTo listOf(fridayId, wednesdayId, mondayId)
    getIds("sort=visitDate,desc").content isEqualTo listOf(mondayInPerson to mondayId, wednesdayVideo to wednesdayId, fridayInPerson to fridayId).sortedByDescending { it.first.visitDate }.map { it.second }
  }

  @Test
  fun `should break sort ties by official visit ID so pages stay consistent`() {
    val pages = (0..2).flatMap { getIds("sort=startTime,desc&page=$it&size=1").content }

    pages isEqualTo listOf(fridayId, mondayId, wednesdayId)
  }

  @Test
  fun `should filter by prisoner number and prison code`() {
    getIds("prisonerNumber=${MOORLAND_PRISONER.number}&prisonCode=$MOORLAND").content isEqualTo listOf(mondayId, wednesdayId, fridayId)
    getIds("prisonerNumber=Z9999ZZ").content isEqualTo emptyList()
    getIds("prisonCode=XXX").content isEqualTo emptyList()
  }

  @Test
  fun `should only return visits within the inclusive date range`() {
    val wednesday = wednesdayVideo.visitDate
    getIds(fromDate = wednesday, toDate = wednesday).content isEqualTo listOf(wednesdayId)
    getIds(fromDate = wednesday.minusDays(1), toDate = wednesday.minusDays(1)).content isEqualTo emptyList()
  }

  @Test
  fun `should filter by visit status`() {
    getIds("visitStatus=CANCELLED").content isEqualTo listOf(fridayId)
    getIds("visitStatus=SCHEDULED").content isEqualTo listOf(mondayId, wednesdayId)
  }

  @Test
  fun `should filter by visit type`() {
    getIds("visitType=VIDEO").content isEqualTo listOf(wednesdayId)
  }

  @Test
  fun `should filter by location ID`() {
    getIds("locationId=${mondayInPerson.dpsLocationId}").content isEqualTo listOf(mondayId, wednesdayId, fridayId)
    getIds("locationId=00000000-0000-0000-0000-000000000000").content isEqualTo emptyList()
  }

  @Test
  fun `should combine filters`() {
    getIds("visitType=IN_PERSON&visitStatus=SCHEDULED&prisonCode=$MOORLAND").content isEqualTo listOf(mondayId)
  }

  @Test
  fun `should reject a to date before the from date`() {
    webTestClient.get()
      .uri("/official-visit/ids?fromDate=2026-11-02&toDate=2026-11-01")
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(REQUIRED_ROLE)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.userMessage").isEqualTo("To date must be on or after the from date")
  }

  @Test
  fun `should reject an unknown sort property`() {
    webTestClient.get()
      .uri("/official-visit/ids?fromDate=2026-11-01&toDate=2026-11-30&sort=notAField")
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(REQUIRED_ROLE)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.userMessage").isEqualTo("Invalid sort property 'notAField'")
  }

  @Test
  fun `should reject a sort property that is not on the allowlist`() {
    webTestClient.get()
      .uri("/official-visit/ids?fromDate=2026-11-01&toDate=2026-11-30&sort=prisonCode")
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(REQUIRED_ROLE)))
      .exchange()
      .expectStatus().isBadRequest
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_OFFICIAL_VISITS_ADMIN", "ROLE_OFFICIAL_VISITS__R", "ROLE_OFFICIAL_VISITS_RW", "ROLE_SOMETHING_ELSE"])
  fun `should be forbidden without the dedicated role`(role: String) {
    webTestClient.get()
      .uri("/official-visit/ids?fromDate=2026-11-01&toDate=2026-11-30")
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(role)))
      .exchange()
      .expectStatus().isForbidden
  }

  private fun getIds(
    filters: String = "",
    fromDate: LocalDate = listOf(mondayInPerson, wednesdayVideo, fridayInPerson).minOf { it.visitDate },
    toDate: LocalDate = listOf(mondayInPerson, wednesdayVideo, fridayInPerson).maxOf { it.visitDate },
  ) = webTestClient.get()
    .uri("/official-visit/ids?fromDate=$fromDate&toDate=$toDate&$filters")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(REQUIRED_ROLE)))
    .exchange()
    .expectStatus().isOk
    .expectBody<IdsResponse>()
    .returnResult().responseBody!!
    .also { assertThat(it.content).doesNotHaveDuplicates() }

  private companion object {
    const val REQUIRED_ROLE = "ROLE_PRISONER_SCHEDULES__RO"
  }

  data class IdsResponse(
    val content: List<Long>,
    val page: PagedModel.PageMetadata,
  )
}
