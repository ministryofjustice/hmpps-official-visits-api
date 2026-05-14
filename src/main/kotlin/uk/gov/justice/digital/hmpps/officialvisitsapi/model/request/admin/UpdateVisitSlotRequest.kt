package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.PositiveOrZero

@Schema(description = "Request to update capacities for a prison visit slot")
data class UpdateVisitSlotRequest(
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
