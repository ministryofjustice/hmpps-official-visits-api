package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The migration response for an official visit and the visitors attending")
data class MigrateVisitResponse(
  @Schema(description = "The pair of IDs for this visit")
  val visit: IdPair,

  @Schema(description = "The list of ID pairs for each visitor on this visit")
  val visitors: List<IdPair> = emptyList(),
)
