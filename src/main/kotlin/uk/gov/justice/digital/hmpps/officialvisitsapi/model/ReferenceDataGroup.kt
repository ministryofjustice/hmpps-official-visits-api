package uk.gov.justice.digital.hmpps.officialvisitsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class ReferenceDataGroup(val displayName: String, val isDocumented: Boolean) {
  DAY("day of the week", true),
  CONTACTS("contact type", true),
  EQUIP_CATEGORY("equipment category", true),
  SEARCH_LEVEL("visitor search type", true),
  TEST_TYPE("test type", false),
  VIS_COMPLETE("visit completion code", true),
  VIS_LOC_TYPE("visit location type", true),
  VIS_STS("visit status code", true),
  VIS_TYPE_CODE("visit type", true),
}
