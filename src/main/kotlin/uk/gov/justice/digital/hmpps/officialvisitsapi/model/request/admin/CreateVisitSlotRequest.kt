package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a new prison visit slot for official visits")
data class CreateVisitSlotRequest(
  @Schema(description = "The DPS location ID where the visit is taking place", example = "aaa-ddd-bbb-123455632323")
  val dpsLocationId: UUID,

  @Schema(description = "Maximum adults allowed in the visit slot")
  val maxAdults: Int? = null,

  @Schema(description = "Maximum groups allowed in the visit slot")
  val maxGroups: Int? = null,

  @Schema(description = "Maximum video sessions allowed in the visit slot")
  val maxVideo: Int? = null,
)
