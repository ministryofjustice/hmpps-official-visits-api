package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ContactDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.VisitorContactInformation

data class VisitorContactInformationDetails(
  val contactId: Long,
  val phoneNumber: String?,
  val emailAddress: String?,
  val fullName: String?,
)

@Service
class VisitorContactInformationService(
  private val personalRelationshipsApiClient: PersonalRelationshipsApiClient,
) {
  fun getVisitorContactInformation(contactIds: List<Long?>): List<VisitorContactInformationDetails> = getVisitorsDetails(contactIds).map { contactDetails ->
    VisitorContactInformationDetails(
      contactId = contactDetails.id,
      phoneNumber = contactDetails.getMainPhoneNumber(),
      emailAddress = contactDetails.getMainEmailAddress(),
      fullName = contactDetails.getFullName(),
    )
  }

  fun visitorContactInformation(contactIds: List<Long?>): VisitorContactInformation {
    val visitorContactInformation = getVisitorContactInformation(contactIds).associateBy { it.contactId }

    return object : VisitorContactInformation {
      override fun phoneNumber(visitorContactId: Long): String? = visitorContactInformation[visitorContactId]?.phoneNumber
      override fun emailAddress(visitorContactId: Long): String? = visitorContactInformation[visitorContactId]?.emailAddress
    }
  }

  fun getVisitorsDetails(contactIds: List<Long?>): List<ContactDetails> = contactIds.filterNotNull().map { contactId ->
    personalRelationshipsApiClient.getContactById(contactId) ?: throw EntityNotFoundException("Contact with ID $contactId not found")
  }

  // Keep parity with existing fallback rules used when building official visit details.
  private fun ContactDetails.getMainPhoneNumber(): String? = if (phoneNumbers.isEmpty()) {
    addresses.singleOrNull { it.primaryAddress || it.mailFlag }?.phoneNumbers?.maxByOrNull { it.createdTime }?.phoneNumber
  } else {
    phoneNumbers.maxByOrNull { it.createdTime }?.phoneNumber
  }

  private fun ContactDetails.getMainEmailAddress(): String? = emailAddresses.takeIf { it.isNotEmpty() }?.maxByOrNull { it.createdTime }?.emailAddress

  private fun ContactDetails.getFullName(): String? = firstName.takeIf { it.isNotEmpty() }?.plus(" ")?.plus(lastName ?: "")
}
