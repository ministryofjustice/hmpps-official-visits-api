package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class NotificationSearchRequest(

  @Schema(description = "The start date filter (optional)", example = "2026-05-01")
  val fromDate: LocalDate? = null,

  @Schema(description = "The end date filter (optional)", example = "2026-05-31")
  val toDate: LocalDate? = null,
)
