package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Class to group together a type of data item and the NOMIS / DPS IDs for it.
 */
data class IdPair(
  @Schema(description = "The category of information returned", example = "PHONE")
  val elementType: ElementType,

  @Schema(description = "The unique ID for this data item provided in the request", example = "123435")
  val nomisId: Long,

  @Schema(description = "The unique ID created in the DPS official visits service", example = "1234")
  val dpsId: Long,
)
