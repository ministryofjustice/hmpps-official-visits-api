package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ReferenceDataItem

fun ReferenceDataEntity.toModel() = ReferenceDataItem(
  referenceDataId = referenceDataId,
  groupCode = groupCode,
  code = code,
  description = description,
  displaySequence = displaySequence,
  enabled = enabled,
)

fun List<ReferenceDataEntity>.toModel() = map { it.toModel() }
