package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Item containing a time slot and its associated visit slots")
data class TimeSlotSummaryItem(
  @Schema(description = "Time Slot")
  val timeSlot: TimeSlot,

  @Schema(description = "List of visit slots associated with time slot")
  val visitSlots: List<VisitSlot>,
)
