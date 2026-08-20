package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class VisitsForReviewCountResponse(
  @Schema(
    description = "The prison code",
    example = "MDI",
  )
  val prisonCode: String,
  @Schema(
    description = "The number of visits for review",
    example = "5",
  )
  val visitsForReviewCount: Long,
)
