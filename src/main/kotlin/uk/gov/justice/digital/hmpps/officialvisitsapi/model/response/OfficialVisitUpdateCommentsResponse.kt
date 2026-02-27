package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class OfficialVisitUpdateCommentsResponse(
  @Schema(description = "The official visit ID")
  val officialVisitId: Long,

  @Schema(description = "The prisoner number")
  val prisonerNumber: String,
)
