package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.MigrateVisitResponse

data class RepairPrisonerVisitsResponse(
  @Schema(description = "The prisoner number for whom visits were replaced", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The list of identifiers for the new visits created")
  val visits: List<MigrateVisitResponse>,
)
