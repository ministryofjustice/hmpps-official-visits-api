package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

class TestApiClient(private val webTestClient: WebTestClient, private val jwtAuthHelper: JwtAuthorisationHelper) {
  fun createOfficialVisit(request: CreateOfficialVisitRequest, prisonUser: PrisonUser) = webTestClient
    .post()
    .uri("/official-visit/prison/${prisonUser.activeCaseLoadId}")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(CreateOfficialVisitResponse::class.java)
    .returnResult().responseBody!!

  private fun setAuthorisation(prisonUser: PrisonUser, roles: List<String>): (HttpHeaders) -> Unit = run {
    jwtAuthHelper.setAuthorisationHeader(
      username = prisonUser.username,
      scope = listOf("read"),
      roles = roles,
    )
  }
}
