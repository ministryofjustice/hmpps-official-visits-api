package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to update capacities for a prison visit slot")
data class UpdateVisitSlotRequest(
  @Schema(description = "Maximum adults allowed in the visit slot")
  val maxAdults: Int? = null,

  @Schema(description = "Maximum groups allowed in the visit slot")
  val maxGroups: Int? = null,

  @Schema(description = "Maximum video sessions allowed in the visit slot")
  val maxVideo: Int? = null,
)
