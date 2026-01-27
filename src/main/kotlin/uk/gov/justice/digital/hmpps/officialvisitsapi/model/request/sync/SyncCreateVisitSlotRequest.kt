package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Request to create a new prison visit slot for official visits")
data class SyncCreateVisitSlotRequest(
  @Schema(description = "Prison time slot Id", required = true)
  val prisonTimeSlotId: Long,

  @Schema(description = "The DPS location ID where the visit is taking place", example = "aaa-ddd-bbb-123455632323")
  val dpsLocationId: UUID,

  @Schema(description = "Maximum adults allowed in the visit slot")
  val maxAdults: Int? = null,

  @Schema(description = "Maximum groups allowed in the visit slot")
  val maxGroups: Int? = null,

  @Schema(description = "Maximum Video sessions allowed in the visit slot")
  val maxVideoSessions: Int? = null,

  @Schema(description = "User who created the entry", example = "admin", required = true)
  val createdBy: String,

  @Schema(description = "The timestamp of when this slot was created", example = "2024-01-01T00:00:00Z")
  val createdTime: LocalDateTime = LocalDateTime.now(),
)
