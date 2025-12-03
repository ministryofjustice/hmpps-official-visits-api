package uk.gov.justice.digital.hmpps.officialvisitsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class ReferenceDataGroup(val displayName: String, val isDocumented: Boolean) {
  ATTENDANCE("attendance code", true),
  DAY("day of the week", true),
  RELATIONSHIP_TYPE("relationship type", true),
  SEARCH_LEVEL("visitor search type", true),
  TEST_TYPE("test type", false),
  VIS_COMPLETION("visit completion code", true),
  VIS_STATUS("visit status code", true),
  VIS_TYPE("visit type code", true),
  VISITOR_TYPE("visitor type code", true),
}
