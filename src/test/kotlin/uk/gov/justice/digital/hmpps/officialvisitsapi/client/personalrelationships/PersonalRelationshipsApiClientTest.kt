package uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock.PersonalRelationshipsApiMockServer

class PersonalRelationshipsApiClientTest {

  private val server = PersonalRelationshipsApiMockServer().also { it.start() }
  private val client = PersonalRelationshipsApiClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get  approved contacts by prisoner Number `() {
    server.stubApprovedContacts("A1234BC")
    val output = client.getApprovedContacts("A1234BC", "O")
    output.size isEqualTo 1
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}
