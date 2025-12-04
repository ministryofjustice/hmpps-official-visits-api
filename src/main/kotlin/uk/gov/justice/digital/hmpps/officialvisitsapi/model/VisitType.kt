package uk.gov.justice.digital.hmpps.officialvisitsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class VisitType {
  IN_PERSON,
  TELEPHONE,
  VIDEO,
  UNKNOWN, // For migrated visits we don't know what type of visit they were
}
