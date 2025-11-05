package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class OfficialVisitor(
  @field:NotBlank(message = "The visitor type code for the official visitor is mandatory")
  @Schema(description = "The visitor type code", example = "CONTACT")
  val visitorTypeCode: String?,

  @field:NotBlank(message = "The contact type code for the official visitor is mandatory")
  @Schema(description = "The contact type code", example = "SOCIAL")
  val contactTypeCode: String?,

  @Schema(description = "The contact ID for the visitor", example = "123456")
  val contactId: Long?,

  @Schema(description = "The prisoner contact ID for the visitor", example = "123456")
  val prisonerContactId: Long?,
)
