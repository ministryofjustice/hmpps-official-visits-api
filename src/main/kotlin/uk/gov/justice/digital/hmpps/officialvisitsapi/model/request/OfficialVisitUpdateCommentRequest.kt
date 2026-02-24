package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class OfficialVisitUpdateCommentRequest(
  @field:Size(max = 240, message = "The staff notes should not exceed {max} characters")
  @Schema(description = "Notes for staff that will not be shared on movement slips", example = "Staff notes")
  val staffNotes: String?,

  @field:Size(max = 240, message = "The prisoner notes should not exceed {max} characters")
  @Schema(description = "Notes for prisoners that may be shared on movement slips", example = "Prisoner notes")
  val prisonerNotes: String?,
)
