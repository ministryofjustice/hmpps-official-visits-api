package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.PositiveOrZero
import java.util.UUID

@Schema(description = "Request to create a new prison visit slot for official visits")
data class CreateVisitSlotRequest(
  @Schema(description = "The DPS location ID where the visit is taking place", example = "123e4567-e89b-12d3-a456-426614174000")
  val dpsLocationId: UUID,

  @Schema(description = "Maximum adults allowed in the visit slot")
  @field:PositiveOrZero
  val maxAdults: Int? = null,

  @Schema(description = "Maximum groups allowed in the visit slot")
  @field:PositiveOrZero
  val maxGroups: Int? = null,

  @Schema(description = "Maximum video sessions allowed in the visit slot")
  @field:PositiveOrZero
  val maxVideo: Int? = null,
)
