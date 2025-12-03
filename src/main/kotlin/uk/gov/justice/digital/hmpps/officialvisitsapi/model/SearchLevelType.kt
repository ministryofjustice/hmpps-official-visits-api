package uk.gov.justice.digital.hmpps.officialvisitsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class SearchLevelType {
  FULL,
  PAT,
  RUB,
  RUB_A,
  RUB_B,
  STR,
}
