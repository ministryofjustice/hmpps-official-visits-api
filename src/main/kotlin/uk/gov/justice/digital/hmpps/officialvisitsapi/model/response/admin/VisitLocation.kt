package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Response for a prison visit location (id and name)")
data class VisitLocation(
  @Schema(description = "DPS location id", required = true)
  val locationId: UUID,

  @Schema(description = "The formatted local name of the location", example = "Legal visits room 8")
  val locationName: String?,
)
