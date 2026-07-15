package uk.gov.justice.digital.hmpps.officialvisitsapi.common

import io.swagger.v3.oas.annotations.media.Schema

data class VisitorAndContactId(
  @Schema(description = "The visitor ID")
  val visitorId: Long,
  @Schema(description = "The contact ID (null when no contact was created)")
  val contactId: Long?,
)
