package uk.gov.justice.digital.hmpps.officialvisitsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class VisitCompletionType {
  NORMAL,
  STAFF_CANCELLED,
  VISITOR_CANCELLED,
  VISITOR_DENIED,
  PRISONER_EARLY,
  VISITOR_EARLY,
  PRISONER_REFUSED,
}
