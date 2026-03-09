package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact

class ContactsServiceTest {
  private val personalRelationshipsApiClient: PersonalRelationshipsApiClient = mock()
  private val contactService = ContactsService(personalRelationshipsApiClient)
  private val prisonerNumber = "A1234AA"
  private val relationshipType = "O"

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `should return approved contacts for a prisoner number and relationship type`() {
    val listOfContacts = listOf(
      prisonerContact(prisonerNumber, relationshipType, true, true, true, null),
    )
    whenever(personalRelationshipsApiClient.getApprovedContacts(prisonerNumber, relationshipType)).thenReturn(listOfContacts)
    assertThat(contactService.getApprovedContacts(prisonerNumber, relationshipType).single().relationshipTypeDescription isEqualTo "Friend")
  }

  @Test
  fun `should return all contacts (inactive and active) where approved and currentTerm`() {
    val listOfContacts = listOf(
      prisonerContact(
        prisonerNumber = prisonerNumber,
        type = relationshipType,
        currentTerm = true,
        isApprovedVisitor = true,
        isRelationshipActive = true,
        contactId = 1L,
        prisonerContactId = 1L,
      ),
      prisonerContact(
        prisonerNumber = prisonerNumber,
        type = relationshipType,
        currentTerm = true,
        isApprovedVisitor = true,
        isRelationshipActive = false,
        contactId = 2L,
        prisonerContactId = 2L,
      ),
    )

    whenever(personalRelationshipsApiClient.getAllPrisonerContacts(prisonerNumber = prisonerNumber, approved = true, currentTerm = true))
      .thenReturn(listOfContacts)

    assertThat(contactService.getAllPrisonerContacts(prisonerNumber, true, true) hasSize 2)
  }

  @Test
  fun `should return empty approved contacts when no contacts are defined`() {
    whenever(personalRelationshipsApiClient.getApprovedContacts(prisonerNumber, relationshipType)).thenReturn(emptyList())
    assertThat(contactService.getApprovedContacts(prisonerNumber, relationshipType) isEqualTo emptyList())
  }
}
