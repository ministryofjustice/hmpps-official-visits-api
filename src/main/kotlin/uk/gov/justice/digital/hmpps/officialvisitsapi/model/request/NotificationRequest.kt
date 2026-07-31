package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.NotificationType

data class NotificationRequest(
  @Schema(description = "The type of notification to send", example = "CREATE")
  val notificationType: NotificationType,

  @Schema(description = "The recipient email address to send the notification to")
  @field:NotEmpty(message = "At least one email address is required")
  @field:NotEmpty(message = "The email address is mandatory")
  val emailAddresses: List<
    @NotBlank(message = "Email addresses must not be blank")
    @Email(message = "Email addresses must be valid email addresses")
    @Size(max = 100, message = "Email addresses must not exceed {max} characters")
    String,
    > = emptyList(),

  @Schema(description = "This could be a PCVL number, a full URL, or some locally-recognised shorthand for the room link.")
  @field:Size(max = 150, message = "Video link URL must not exceed {max} characters")
  val videoLinkUrl: String? = null,

  @Schema(description = "Some personalised email notes about the visit.")
  @field:Size(max = 240, message = "Notes must not exceed {max} characters")
  val notes: String? = null,
)
