package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ContactDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ContactEmailDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ContactPhoneDetails
import java.time.LocalDateTime

class VisitorContactInformationServiceTest {
  private val personalRelationshipsApiClient: PersonalRelationshipsApiClient = mock()
  private val service = VisitorContactInformationService(personalRelationshipsApiClient)

  @Test
  fun `should return contact details for non null contact ids`() {
    val contact = contactDetails(id = 11L)
    whenever(personalRelationshipsApiClient.getContactById(11L)).thenReturn(contact)

    val result = service.getVisitorsDetails(listOf(null, 11L, null))

    assertThat(result).containsExactly(contact)
  }

  @Test
  fun `should resolve latest phone and email for each contact`() {
    val olderTime = LocalDateTime.now().minusDays(1)
    val newerTime = LocalDateTime.now()

    whenever(personalRelationshipsApiClient.getContactById(21L)).thenReturn(
      contactDetails(
        id = 21L,
        phoneNumbers = listOf(
          ContactPhoneDetails(contactPhoneId = 1, contactId = 21L, phoneType = "MOB", phoneTypeDescription = "Mobile", phoneNumber = "0111", createdBy = "test", createdTime = olderTime),
          ContactPhoneDetails(contactPhoneId = 2, contactId = 21L, phoneType = "MOB", phoneTypeDescription = "Mobile", phoneNumber = "0222", createdBy = "test", createdTime = newerTime),
        ),
        emailAddresses = listOf(
          ContactEmailDetails(contactEmailId = 1, contactId = 21L, emailAddress = "old@example.com", createdBy = "test", createdTime = olderTime),
          ContactEmailDetails(contactEmailId = 2, contactId = 21L, emailAddress = "new@example.com", createdBy = "test", createdTime = newerTime),
        ),
      ),
    )

    val result = service.getVisitorContactInformation(listOf(21L))

    assertThat(result).containsExactly(
      VisitorContactInformationDetails(contactId = 21L, phoneNumber = "0222", emailAddress = "new@example.com", "John Doe"),
    )
  }

  @Test
  fun `should throw when a contact id does not exist`() {
    whenever(personalRelationshipsApiClient.getContactById(999L)).thenReturn(null)

    val error = assertThrows<EntityNotFoundException> {
      service.getVisitorsDetails(listOf(999L))
    }

    assertThat(error.message).isEqualTo("Contact with ID 999 not found")
  }

  private fun contactDetails(
    id: Long,
    phoneNumbers: List<ContactPhoneDetails> = emptyList(),
    emailAddresses: List<ContactEmailDetails> = emptyList(),
  ) = ContactDetails(
    id = id,
    lastName = "Doe",
    firstName = "John",
    isStaff = false,
    interpreterRequired = false,
    addresses = emptyList(),
    phoneNumbers = phoneNumbers,
    emailAddresses = emailAddresses,
    identities = emptyList(),
    employments = emptyList(),
    createdBy = "test",
    createdTime = LocalDateTime.now(),
    titleCode = "MR",
    titleDescription = "Mr",
    middleNames = null,
    dateOfBirth = null,
    deceasedDate = null,
    languageCode = null,
    languageDescription = null,
    domesticStatusCode = null,
    domesticStatusDescription = null,
    genderCode = null,
    genderDescription = null,
    staff = null,
  )
}
