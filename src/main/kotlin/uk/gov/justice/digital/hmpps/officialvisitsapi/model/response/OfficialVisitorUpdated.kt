package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class OfficialVisitorUpdated(
  @Schema(description = "The official visitor id")
  val officialVisitorId: Long,

  @Schema(description = "Contact id of the official visitor")
  val contactId: Long,
)
