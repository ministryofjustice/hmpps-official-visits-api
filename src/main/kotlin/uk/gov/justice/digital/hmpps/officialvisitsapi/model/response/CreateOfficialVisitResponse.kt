package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class CreateOfficialVisitResponse(
  @Schema(description = "The official visit ID")
  val officialVisitId: Long,
  @Schema(description = "The prisoner number")
  val prisonerNumber: String,
  @Schema(description = "The visitor and contact IDs")
  val visitorAndContactIds: List<VisitorAndContactId>,
)

data class VisitorAndContactId(
  @Schema(description = "The visitor ID")
  val visitorId: Long,
  @Schema(description = "The contact ID (null when no contact was created)")
  val contactId: Long?,
)
