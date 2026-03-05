package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerContact

class ContactsControllerIntegrationTest : IntegrationTestBase() {

  @Test
  fun `should get all approved contacts for official visits with the valid prisoner number`() {
    personalRelationshipsApi().stubApprovedContacts("A1234BC")
    webTestClient.approvedContents(prisonerNumber = "A1234BC") hasSize 1
  }

  @Test
  fun `should get all contacts for official visits with for prisoner number`() {
    personalRelationshipsApi().stubAllContacts("A1234BC", prisonerContacts = listOf(prisonerContact("A1234BC", type = "O")))
    webTestClient.allContacts(prisonerNumber = "A1234BC") hasSize 1
  }

  private fun WebTestClient.approvedContents(prisonerNumber: String) = this
    .get()
    .uri("/prisoner/$prisonerNumber/approved-relationships?relationshipType=O")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList<ApprovedContact>()
    .returnResult().responseBody!!

  private fun WebTestClient.allContacts(prisonerNumber: String) = this
    .get()
    .uri("/prisoner/$prisonerNumber/all-contacts")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList<PrisonerContact>()
    .returnResult().responseBody!!
}
