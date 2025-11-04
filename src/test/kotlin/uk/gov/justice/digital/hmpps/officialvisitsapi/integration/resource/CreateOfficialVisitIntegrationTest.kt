package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse

class CreateOfficialVisitIntegrationTest : IntegrationTestBase() {
  @Test
  fun `happy path - create official visit`() {
    webTestClient.create(CreateOfficialVisitRequest(prisonerNumber = "1234567"))
  }

  private fun WebTestClient.create(request: CreateOfficialVisitRequest) = this
    .post()
    .uri("/official-visit")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(CreateOfficialVisitResponse::class.java)
    .returnResult().responseBody!!
}
