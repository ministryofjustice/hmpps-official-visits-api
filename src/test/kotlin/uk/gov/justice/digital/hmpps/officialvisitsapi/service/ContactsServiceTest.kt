package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import java.time.LocalDate

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
  fun `getApprovedContacts should return approved contacts for valid prisonerNumber and relationship type`() {
    val listOfCodes = listOf(
      prisonerContact(prisonerNumber, relationshipType, true, true, true, null),
    )
    whenever(personalRelationshipsApiClient.getApprovedContacts(prisonerNumber, relationshipType)).thenReturn(listOfCodes)
    assertThat(contactService.getApprovedContacts(prisonerNumber, relationshipType).single().relationshipTypeDescription isEqualTo "Friend")
  }

  @Test
  fun `getApprovedContacts should return empty  approved contacts when currentTerm is false `() {
    val listOfCodes = listOf(
      prisonerContact(prisonerNumber, relationshipType, false, true, true, null),
    )
    whenever(personalRelationshipsApiClient.getApprovedContacts(prisonerNumber, relationshipType)).thenReturn(emptyList())
    assertThat(contactService.getApprovedContacts(prisonerNumber, relationshipType) isEqualTo emptyList())
  }

  @Test
  fun `getApprovedContacts should return empty  approved contacts when deceasedDate is not  `() {
    val listOfCodes = listOf(
      prisonerContact(prisonerNumber, relationshipType, true, true, true, LocalDate.now()),
    )
    whenever(personalRelationshipsApiClient.getApprovedContacts(prisonerNumber, relationshipType)).thenReturn(emptyList())
    assertThat(contactService.getApprovedContacts(prisonerNumber, relationshipType) isEqualTo emptyList())
  }
}
