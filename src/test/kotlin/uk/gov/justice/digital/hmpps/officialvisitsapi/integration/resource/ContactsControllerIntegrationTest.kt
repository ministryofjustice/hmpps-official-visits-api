package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact

class ContactsControllerIntegrationTest : IntegrationTestBase() {

  @Test
  fun `should get all approved contacts for official visits with the valid prisoner number`() {
    personalRelationshipsApi().stubApprovedContacts("A1234BC")
    val response = webTestClient.approvedContents(prisonerNumber = "A1234BC", relationshipType = "O")
    response.size isEqualTo 1
  }

  private fun WebTestClient.approvedContents(prisonerNumber: String, relationshipType: String) = this
    .get()
    .uri("/prisoner/$prisonerNumber/contact-relationships?relationshipType=$relationshipType")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_CONTACTS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ApprovedContact::class.java)
    .returnResult().responseBody!!
}
