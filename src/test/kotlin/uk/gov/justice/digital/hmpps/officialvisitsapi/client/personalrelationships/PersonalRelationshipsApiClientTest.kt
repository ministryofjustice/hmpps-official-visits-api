package uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock.PersonalRelationshipsApiMockServer

class PersonalRelationshipsApiClientTest {
  private val server = PersonalRelationshipsApiMockServer().also { it.start() }
  private val client = PersonalRelationshipsApiClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get approved contacts by prisoner number`() {
    server.stubApprovedContacts("A1234BC")
    val output = client.getApprovedContacts("A1234BC", "O")
    output.size isEqualTo 1
    output.single().relationshipTypeDescription isEqualTo "Friend"
  }

  @Test
  fun `should get a list of reference codes by reference group`() {
    server.stubReferenceGroup()
    val output = client.getReferenceDataByGroup(ReferenceCodeGroup.OFFICIAL_RELATIONSHIP.toString())
    output?.size isEqualTo 1
    output?.single()?.groupCode isEqualTo ReferenceCodeGroup.OFFICIAL_RELATIONSHIP
  }

  @Test
  fun `should get a list of prisoner contact relationships by prisoner and contact ID`() {
    server.stubPrisonerContactRelationships(prisonerNumber = "A1234BC", contactId = 12345)
    val output = client.getPrisonerContactRelationships(prisonerNumber = "A1234BC", contactId = 12345)
    output.size isEqualTo 1
    output.single().isRelationshipActive isEqualTo true
    output.single().relationshipToPrisonerCode isEqualTo "POL"
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}
