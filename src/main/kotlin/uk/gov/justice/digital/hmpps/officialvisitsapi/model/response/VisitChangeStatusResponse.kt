package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Response indicating whether an official visit has been modified since the last notification was sent.
 */
data class VisitChangeStatusResponse(
  @Schema(
    description = "Whether the visit details have changed since the last notification was sent",
    example = "true",
  )
  val hasChanged: Boolean,
)
