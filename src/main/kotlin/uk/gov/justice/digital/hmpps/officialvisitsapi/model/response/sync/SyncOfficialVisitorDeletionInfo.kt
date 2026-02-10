package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

data class SyncOfficialVisitorDeletionInfo(

  @Schema(description = "The official visitor ID", example = "111111")
  val officialVisitorId: Long,

  @Schema(description = "The contact ID", example = "111111")
  val contactId: Long?,
)
