package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitRequest

data class RepairPrisonerVisitsRequest(
  @Schema(description = "A list of visits to create for the prisoner")
  val visits: List<MigrateVisitRequest>,
)
