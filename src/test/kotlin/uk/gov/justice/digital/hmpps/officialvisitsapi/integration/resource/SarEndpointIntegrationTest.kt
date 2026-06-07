package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.TestConfiguration
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar.SubjectAccessResponseData
import java.time.LocalDate

/**
 * These tests check that the SAR endpoint returns the expected data.
 * They do not check that the SAR template is rendered Ok or that it contains the correct information.
 */

@Import(TestConfiguration::class)
class SarEndpointIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun `set up official visit and change history data`() {
    // Stub prisoner
    // Stub contacts/relationships
  }

  @Test
  fun `SAR endpoint should return expected data`() {
    // Create a visit
    // Amend the visit
    // Cancel tne visit

    // val response = webTestClient.getSarContent(pentonvillePrisoner.number, yesterday(), today())

    // assert visit content
    // assert visitor content
    // assert audit content
  }

  private fun WebTestClient.getSarContent(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = get()
    .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<SubjectAccessRequestContent>()
    .returnResult().responseBody!!

  @Test
  fun `SAR API should return a 204 no content when no data`() {
    webTestClient.noSarContent("B1234BB", LocalDate.now().minusDays(1), LocalDate.now())
  }

  private fun WebTestClient.noSarContent(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = get()
    .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isNoContent
}

data class SubjectAccessRequestContent(val content: SubjectAccessResponseData)
