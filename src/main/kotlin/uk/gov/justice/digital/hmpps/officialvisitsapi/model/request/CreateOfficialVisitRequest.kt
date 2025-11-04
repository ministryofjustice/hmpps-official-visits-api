package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class CreateOfficialVisitRequest(
  @field:NotBlank(message = "Prisoner number is mandatory")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  val prisonerNumber: String?,
)
