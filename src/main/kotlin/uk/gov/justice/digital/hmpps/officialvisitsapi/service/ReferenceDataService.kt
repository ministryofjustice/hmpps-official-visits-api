package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.validation.ValidationException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ReferenceDataItem
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.ReferenceDataRepository

@Service
class ReferenceDataService(private val referenceDataRepository: ReferenceDataRepository) {

  fun getReferenceDataByGroup(groupCode: ReferenceDataGroup, sort: Sort, activeOnly: Boolean): List<ReferenceDataItem> = if (activeOnly) {
    referenceDataRepository.findAllByGroupCodeAndEnabledEquals(groupCode, true, sort).toModel()
  } else {
    referenceDataRepository.findAllByGroupCodeEquals(groupCode, sort).toModel()
  }

  fun getReferenceDataByGroupAndCode(groupCode: ReferenceDataGroup, code: String): ReferenceDataItem? = referenceDataRepository.findByGroupCodeAndCode(groupCode, code)?.toModel()

  fun validateReferenceData(groupCode: ReferenceDataGroup, code: String, allowInactive: Boolean): ReferenceDataItem {
    val referenceDataItem = getReferenceDataByGroupAndCode(groupCode, code) ?: throw ValidationException("Unsupported ${groupCode.displayName} ($code)")
    if (!allowInactive && !referenceDataItem.enabled) {
      throw ValidationException("Unsupported ${groupCode.displayName} ($code). This code is no longer active.")
    }
    return referenceDataItem
  }
}
