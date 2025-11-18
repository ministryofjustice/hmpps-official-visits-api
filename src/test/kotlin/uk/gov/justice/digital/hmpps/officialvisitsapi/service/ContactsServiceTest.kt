package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContacts
class ContactsServiceTest {

  private val personalRelationshipsApiClient: PersonalRelationshipsApiClient = mock()
  private val contactService = ContactsService(personalRelationshipsApiClient)

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `getApprovedContacts should return approved contacts for valid prisonerNumber and relationship type`() {
    val listOfCodes = listOf(
      prisonerContacts("ABCD", "O"),
    )
    whenever(personalRelationshipsApiClient.getApprovedContacts("ABCD", "O")).thenReturn(listOfCodes)
    assertThat(contactService.getApprovedContacts("ABCD", "O").single().relationshipTypeDescription isEqualTo "Friend")
  }
}
