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
  STAFF_EARLY,
  PRISONER_CANCELLED,
  VISITOR_NO_SHOW,
}
