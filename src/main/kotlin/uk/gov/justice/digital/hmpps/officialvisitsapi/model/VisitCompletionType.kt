package uk.gov.justice.digital.hmpps.officialvisitsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class VisitCompletionType(val isCancellation: Boolean) {
  NORMAL(false),
  PRISONER_EARLY(false),
  PRISONER_REFUSED(false),
  STAFF_EARLY(false),
  VISITOR_DENIED(false),
  VISITOR_EARLY(false),
  VISITOR_NO_SHOW(false),
  PRISONER_CANCELLED(true),
  STAFF_CANCELLED(true),
  VISITOR_CANCELLED(true),
}
