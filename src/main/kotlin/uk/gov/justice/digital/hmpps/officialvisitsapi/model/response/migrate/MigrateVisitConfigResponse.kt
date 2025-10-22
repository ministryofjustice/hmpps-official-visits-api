package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The migration response for an official visit time slot and its visit slots")
data class MigrateVisitConfigResponse(
  @Schema(description = "The 3-letter agy_loc_id from NOMIS where this time slot exists", example = "MDI", required = true)
  val prisonCode: String,

  @Schema(description = "The 3-letter day of the week code where this slot exists in this prison.", example = "TUE", required = true)
  val dayCode: String,

  @Schema(description = "The time slot sequence from NOMIS", example = "1", required = true)
  val timeSlotSeq: Long,

  @Schema(description = "The ID created in DPS for this prison time slot", example = "123456")
  val dpsTimeSlotId: Long,

  @Schema(description = "List of NOMIS and DPS IDs for the visit slots created")
  val visitSlots: List<IdPair> = emptyList(),
)
