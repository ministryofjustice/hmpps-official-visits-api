package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

data class SyncOfficialVisitDeletionInfo(
  @Schema(description = "The official visit ID", example = "111111")
  val officialVisitId: Long,

  @Schema(description = "The prison code", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The prisoner number")
  val prisonerNumber: String,

  @Schema(description = "List of deleted official visitors")
  val visitors: List<SyncOfficialVisitorDeletionInfo>,
)
