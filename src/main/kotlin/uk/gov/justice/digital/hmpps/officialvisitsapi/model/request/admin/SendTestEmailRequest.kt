package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request to send a test email notification")
data class SendTestEmailRequest(
  @field:Schema(description = "Email address of the recipient", example = "recipient@example.com", required = true)
  @field:Email
  @field:NotBlank
  val recipientEmailAddress: String,

  @field:Schema(description = "Gov Notify template id", example = "a0823218-91dd-4cf0-9835-4b90024f62c8", required = true)
  @field:NotBlank
  val templateId: String,

  @field:Schema(description = "Template personalisation key/value pairs")
  val personalisation: Map<String, String> = emptyMap(),

  @field:Schema(description = "Optional reply-to email id configured in Gov Notify", example = "reply-to-id")
  val replyToEmailId: String? = null,

  @field:Schema(description = "Optional client reference attached to the notification", example = "official-visits-test-email")
  val reference: String? = null,
)
