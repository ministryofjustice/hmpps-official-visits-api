package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType

data class OfficialVisitor(
  @field:NotBlank(message = "The visitor type code for the official visitor is mandatory")
  @Schema(description = "The visitor type code (CONTACT, OPV, PRISONER)", example = "CONTACT")
  val visitorTypeCode: VisitorType?,

  @Schema(description = "The contact ID for the visitor", example = "123456")
  val contactId: Long?,

  @Schema(description = "The prisoner contact ID for the visitor", example = "123456")
  val prisonerContactId: Long?,

  @field:NotBlank(message = "The relationship to the prisoner code is mandatory")
  @Schema(description = "The relationship code", example = "POM")
  val relationshipCode: String? = null,

  @Schema(description = "Set to true if this is the lead visitor", example = "false")
  val leadVisitor: Boolean? = false,

  @Schema(description = "Set to true if this visitor requires an assisted visit", example = "false")
  val assistedVisit: Boolean? = false,

  @Schema(description = "Any notes to keep about this visitor", example = "notes")
  val visitorNotes: String? = null,
)
