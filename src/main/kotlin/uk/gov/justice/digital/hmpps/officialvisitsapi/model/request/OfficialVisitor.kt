package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
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

  @field:Size(max = 240, message = "The visitor notes should not exceed {max} characters")
  @Schema(description = "Any required assistance notes to keep about this visitor.  Will be ignored if assisted visit is not true", example = "Wheelchair access required")
  val assistedNotes: String? = null, // assisted notes are only applicable when assistedVisit == true

  @Valid
  @Schema(description = "Details of any equipment the visitor will bring to the visit.")
  val visitorEquipment: VisitorEquipment? = null,
)

@Schema(description = "Represents details of any equipment the visitor will bring to the visit.")
data class VisitorEquipment(
  @field:NotBlank(message = "The equipment description is mandatory.")
  @field:Size(max = 240, message = "The equipment description should not exceed {max} characters")
  @Schema(description = "A description of the equipment the visitor will bring", example = "Laptop")
  val description: String?,
)
