package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

data class SyncTimeSlotAndVisitSlots(
  @Schema(description = "Time Slot")
  val timeSlot: SyncTimeSlot,

  @Schema(description = "List of visit slots associated with time slot")
  val visitSlots: List<SyncVisitSlot>,
)
