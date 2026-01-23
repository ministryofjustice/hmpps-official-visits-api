package uk.gov.justice.digital.hmpps.officialvisitsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class DayType {
  MON,
  TUE,
  WED,
  THU,
  FRI,
  SAT,
  SUN,
}
