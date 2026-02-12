package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Admin response for Summary of time slots and associated visit slots for the prison")
data class TimeSlotSummary(
  @Schema(description = "Prison code", example = "MDI")
  val prisonCode: String,

  @Schema(description = "Prison name", example = "Moorland (HMP & YOI)")
  val prisonName: String,

  @Schema(description = "List of all time slots and associated visit slots for the prison")
  val timeSlots: List<TimeSlotSummaryItem>,
)
