package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ReferenceDataItem
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.ReferenceDataRepository

@Service
@Transactional(readOnly = true)
class ReferenceDataService(private val referenceDataRepository: ReferenceDataRepository) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getReferenceDataByGroup(groupCode: ReferenceDataGroup, sort: Sort, activeOnly: Boolean): List<ReferenceDataItem> = if (activeOnly) {
    logger.info("Find reference codes for $groupCode sorting: $sort activeOnly: $activeOnly")
    referenceDataRepository.findAllByGroupCodeAndEnabledEquals(groupCode, true, sort).toModel()
  } else {
    logger.info("Find reference codes for $groupCode sorting: $sort activeOnly: $activeOnly")
    referenceDataRepository.findAllByGroupCodeEquals(groupCode, sort).toModel()
  }

  @Cacheable(CacheConfiguration.REFERENCE_DATA_BY_GROUP_AND_CODE_CACHE)
  fun getReferenceDataByGroupAndCode(groupCode: ReferenceDataGroup, code: String): ReferenceDataItem? = referenceDataRepository.findByGroupCodeAndCode(groupCode, code)?.toModel()

  fun validateReferenceData(groupCode: ReferenceDataGroup, code: String, allowInactive: Boolean): ReferenceDataItem {
    val referenceDataItem = referenceDataRepository.findByGroupCodeAndCode(groupCode, code)?.toModel() ?: throw ValidationException("Unsupported ${groupCode.displayName} ($code)")
    if (!allowInactive && !referenceDataItem.enabled) {
      throw ValidationException("Unsupported ${groupCode.displayName} ($code). This code is no longer active.")
    }
    return referenceDataItem
  }
}
