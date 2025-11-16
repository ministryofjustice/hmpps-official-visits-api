package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationship.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase

class ContentControllerIntegrationTest : IntegrationTestBase() {

  @Test
  fun `should perform basic GET with no visits`() {
    personalRelationshipsApi().stubApprovedContacts("A1234BC")
    val response = webTestClient.approvedContents(prisonerNumber = "A1234BC", type = "O")
    response.size isEqualTo 1
  }

  private fun WebTestClient.approvedContents(prisonerNumber: String, type: String) = this
    .get()
    .uri("/prisoner-contact/prison/$prisonerNumber/contact-relationships?type=$type")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_CONTACTS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(PrisonerContactSummary::class.java)
    .returnResult().responseBody!!
}
