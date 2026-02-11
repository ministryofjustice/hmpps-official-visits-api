package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Sync response for a prison visit slot")
data class VisitSlot(
  @Schema(description = "Prison visit slot Id", required = true)
  val visitSlotId: Long,

  @Schema(description = "Prison Code")
  val prisonCode: String,

  @Schema(description = "Prison time slot Id", required = true)
  val prisonTimeSlotId: Long,

  @Schema(description = "The DPS location ID where the visit is taking place", example = "aaa-ddd-bbb-123455632323")
  val dpsLocationId: UUID,

  @Schema(description = "The description of the prison location this visit slot is in", example = "Legal visits room 8")
  var locationDescription: String? = null,

  @Schema(description = "The type of the prison location this visit slot is in, e.g. 'VISIT', 'VIDEO_CALL', 'OTHER'", example = "VISIT")
  var locationType: String? = null,

  @Schema(description = "The capacity of the prison location this visit slot is in, if known", example = "12")
  var locationCapacity: Int? = null,

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
