package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PrisonerContactDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactRelationship
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toPrisonerContactModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerContact

@Service
class ContactsService(private val personalRelationshipsApiClient: PersonalRelationshipsApiClient) {

  fun getApprovedContacts(prisonerNumber: String, relationshipType: String): List<ApprovedContact> = run {
    personalRelationshipsApiClient.getApprovedContacts(prisonerNumber, relationshipType)
      .filter { it.currentTerm && it.isApprovedVisitor && it.deceasedDate == null }.toModel()
  }

  fun getApprovedContacts(prisonerNumber: String): List<ApprovedContact> = run {
    personalRelationshipsApiClient.getApprovedContacts(prisonerNumber)
      .filter { it.currentTerm && it.isApprovedVisitor && it.deceasedDate == null }.toModel()
  }

  fun getPrisonerContactSummary(prisonerNumber: String, contactId: Long): List<PrisonerContactSummary> = run {
    personalRelationshipsApiClient.getPrisonerContactRelationships(prisonerNumber, contactId)
  }

  fun getAllPrisonerContacts(prisonerNumber: String, approved: Boolean?, currentTerm: Boolean?): List<PrisonerContact> = run {
    personalRelationshipsApiClient.getAllPrisonerContacts(prisonerNumber, approved, currentTerm).toPrisonerContactModel()
  }

  fun getPrisonerContactRelationships(prisonerContacts: Set<PrisonerContactDto>): Map<String, Collection<PrisonerContactRelationship>> = run {
    personalRelationshipsApiClient.getPrisonerContactRelationships(prisonerContacts).groupBy { it.prisonerNumber }
  }
}
