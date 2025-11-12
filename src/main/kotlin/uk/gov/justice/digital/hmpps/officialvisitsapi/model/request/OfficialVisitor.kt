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

  @Schema(description = "The first name of the contact", example = "Bob")
  val firstName: String?,

  @Schema(description = "The last name of the contact", example = "Harris")
  val lastName: String?,

  @Schema(description = "Set to true if this is the lead visitor", example = "false")
  val leadVisitor: Boolean? = false,

  @Schema(description = "Set to true if this visitor requires an assisted visit", example = "false")
  val assistedVisit: Boolean? = false,

  @Schema(description = "The email address for this visitor", example = "bob@harris.com")
  val emailAddress: String? = null,

  @Schema(description = "The phone number for this visitor", example = "09090 9090900")
  val phoneNumber: String? = null,

  @Schema(description = "Any notes to keep about this visitor", example = "notes")
  val visitorNotes: String? = null,
)
