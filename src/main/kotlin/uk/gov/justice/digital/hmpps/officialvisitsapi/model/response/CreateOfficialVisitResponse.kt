package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.VisitorAndContactId

data class CreateOfficialVisitResponse(
  @Schema(description = "The official visit ID")
  val officialVisitId: Long,
  @Schema(description = "The prisoner number")
  val prisonerNumber: String,
  @Schema(description = "The visitor and contact IDs")
  val visitorAndContactIds: List<VisitorAndContactId>,
)
