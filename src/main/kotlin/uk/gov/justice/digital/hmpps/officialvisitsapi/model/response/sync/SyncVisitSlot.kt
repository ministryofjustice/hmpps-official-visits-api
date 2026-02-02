package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Sync response for a prison visit slot")
data class SyncVisitSlot(
  @Schema(description = "Prison visit slot Id", required = true)
  val visitSlotId: Long,

  @Schema(description = "Prison Code")
  val prisonCode: String,

  @Schema(description = "Prison time slot Id", required = true)
  val prisonTimeSlotId: Long,

  @Schema(description = "The DPS location ID where the visit is taking place", example = "aaa-ddd-bbb-123455632323")
  val dpsLocationId: UUID,

  @Schema(description = "Maximum adults allowed in the visit slot")
  val maxAdults: Int? = null,

  @Schema(description = "Maximum groups allowed in the visit slot")
  val maxGroups: Int? = null,

  @Schema(description = "Username who created the visit slot", example = "admin")
  val createdBy: String,

  @Schema(description = "The timestamp of when this visit slot was created", example = "2024-01-01T00:00:00Z")
  val createdTime: LocalDateTime,

  @Schema(description = "Username who last updated the visit slot", example = "admin")
  val updatedBy: String? = null,

  @Schema(description = "The timestamp of when this visit slot was last updated", example = "2024-01-01T00:00:00Z")
  val updatedTime: LocalDateTime? = null,
)
