package uk.gov.justice.digital.hmpps.officialvisitsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class ReferenceDataGroup(val displayName: String, val isDocumented: Boolean) {
  VIS_STS("visit status", true),
  VIS_COMPLETE("visit completion reasons", true),
  VIS_TYPE_CODE("visit types", true),
  VIS_LOC_CODE("visit location types", true),
  SEARCH_TYPE("visitor search types", true),
  EQUIP_CATEGORY("equipment categories", true),
  CONTACTS("contact types", true),
  DAY("days of the week", true),
  TEST_TYPE("test type", false),
}
