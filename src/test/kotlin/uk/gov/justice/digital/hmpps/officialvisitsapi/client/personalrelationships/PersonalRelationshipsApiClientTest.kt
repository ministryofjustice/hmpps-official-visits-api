package uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
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
    output?.size isEqualTo 2
    output?.first()?.groupCode isEqualTo ReferenceCodeGroup.OFFICIAL_RELATIONSHIP
  }

  @Test
  fun `should get a list of prisoner contact relationships by prisoner and contact ID`() {
    server.stubPrisonerContactRelationships(prisonerNumber = "A1234BC", contactId = 12345)
    val output = client.getPrisonerContactRelationships(prisonerNumber = "A1234BC", contactId = 12345)
    output.size isEqualTo 1
    output.single().isRelationshipActive isEqualTo true
    output.single().relationshipToPrisonerCode isEqualTo "POL"
  }

  @Nested
  inner class AllPrisonerContacts {
    private val allContacts = listOf(
      prisonerContact("A1234BC", type = "O", currentTerm = true),
      prisonerContact("A1234BC", type = "O", currentTerm = false),
      prisonerContact("A1234BC", type = "S", currentTerm = true),
      prisonerContact("A1234BC", type = "S", currentTerm = false),
    )

    @Test
    fun `should get all prisoner contacts for prisoner number`() {
      server.stubAllContacts(prisonerNumber = "A1234BC", prisonerContacts = allContacts)

      with(client.getAllPrisonerContacts(prisonerNumber = "A1234BC")) {
        size isEqualTo 4
      }
    }

    @Test
    fun `should get current term prisoner contacts only for prisoner number`() {
      server.stubAllContacts(prisonerNumber = "A1234BC", prisonerContacts = allContacts)

      val currentTermContacts = client.getAllPrisonerContacts(prisonerNumber = "A1234BC", currentTerm = true)

      currentTermContacts.size isEqualTo 2
      currentTermContacts.map { it.currentTerm } isEqualTo listOf(true, true)
    }

    @Test
    fun `should get non-current term prisoner contacts only for prisoner number`() {
      server.stubAllContacts(prisonerNumber = "A1234BC", prisonerContacts = allContacts)

      val currentTermContacts = client.getAllPrisonerContacts(prisonerNumber = "A1234BC", currentTerm = false)

      currentTermContacts.size isEqualTo 2
      currentTermContacts.map { it.currentTerm } isEqualTo listOf(false, false)
    }

    @Test
    fun `should be no prisoner contacts for prisoner number`() {
      server.stubAllContacts(prisonerNumber = "A1234BC", prisonerContacts = emptyList())

      client.getAllPrisonerContacts(prisonerNumber = "A1234BC", currentTerm = false) hasSize 0
    }
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}
