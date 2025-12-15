package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCode
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCodeGroup

@Service
class PersonalRelationshipsReferenceDataService(
  private val personalRelationshipsApiClient: PersonalRelationshipsApiClient,
) {
  fun getReferenceDataByGroupCode(groupCode: ReferenceCodeGroup): List<ReferenceCode>? = run {
    personalRelationshipsApiClient.getReferenceDataByGroup(groupCode.toString())
  }

  fun getReferenceDataByCode(groupCode: String, code: String): ReferenceCode? = run {
    personalRelationshipsApiClient.getReferenceDataByGroup(groupCode)?.first { it.code == code }
  }
}
