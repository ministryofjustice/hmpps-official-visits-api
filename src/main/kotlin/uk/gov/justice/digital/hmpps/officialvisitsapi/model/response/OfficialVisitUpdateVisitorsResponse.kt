package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class OfficialVisitUpdateVisitorsResponse(
  @Schema(description = "The official visit ID")
  val officialVisitId: Long,

  @Schema(description = "The prisoner code")
  val prisonCode: String,

  @Schema(description = "The prisoner number")
  val prisonerNumber: String,

  @Schema(description = "The list of new visitors")
  var visitorsAdded: List<OfficialVisitorUpdated>,

  @Schema(description = "The list of deleted visitors")
  var visitorsDeleted: List<OfficialVisitorUpdated>,

  @Schema(description = "The list of updated visitors")
  var visitorsUpdated: List<OfficialVisitorUpdated>,
)
