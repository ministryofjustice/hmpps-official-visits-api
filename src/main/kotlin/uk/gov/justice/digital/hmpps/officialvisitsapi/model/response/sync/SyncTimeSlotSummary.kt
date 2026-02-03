package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Sync response for Summary of time slots and associated visit slots for the prison")
data class SyncTimeSlotSummary(
  @Schema(description = "Prison code", example = "MDI")
  val prisonCode: String,

  @Schema(description = "List of all time slots and associated visit lots for the prison")
  val timeSlots: List<SyncTimeSlotSummaryItem>,
)
