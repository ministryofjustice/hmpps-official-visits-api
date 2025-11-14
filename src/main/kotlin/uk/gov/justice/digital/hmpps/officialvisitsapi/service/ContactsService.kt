package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationship.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient

@Service
class ContactsService(private val personalRelationshipsApiClient: PersonalRelationshipsApiClient) {

  fun getApprovedContacts(prisonerNumber: String, type: String): List<PrisonerContactSummary> = personalRelationshipsApiClient.getApprovedContacts(prisonerNumber, type)
}
