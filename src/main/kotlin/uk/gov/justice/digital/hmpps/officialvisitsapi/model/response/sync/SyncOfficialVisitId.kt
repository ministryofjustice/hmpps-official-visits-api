package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object for sync reconciliation")
data class SyncOfficialVisitId(
  @Schema(description = "The ID for an Official visits", example = "111111")
  val officialVisitId: Long,
)
