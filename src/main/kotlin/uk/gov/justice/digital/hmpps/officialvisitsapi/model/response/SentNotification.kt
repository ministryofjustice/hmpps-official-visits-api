package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class SentNotification(
  @Schema(description = "The official visit ID", example = "123")
  val officialVisitId: Long,

  @Schema(description = "The date the email was sent (YYYY-MM-DD format)", example = "2026-05-22")
  val sentDate: String,

  @Schema(description = "The full date and time the email was sent (ISO 8601 format)", example = "2026-05-22T10:30:00")
  val sentDateTime: String,

  @Schema(description = "The visit date (YYYY-MM-DD format)", example = "2026-06-01")
  val visitDate: String,

  @Schema(description = "The visit start time (HH:MM format)", example = "09:00")
  val visitStartTime: String,

  @Schema(description = "The visit end time (HH:MM format)", example = "10:00")
  val visitEndTime: String,

  @Schema(description = "The first name of the visitor", example = "Bob", nullable = true)
  val firstName: String? = null,

  @Schema(description = "The last name of the visitor", example = "Harris", nullable = true)
  val lastName: String? = null,

  @Schema(description = "The prisoner's number", example = "G1234AB")
  val prisonerNumber: String,

  @Schema(description = "The email address the notification was sent to", example = "user@example.com")
  val emailAddress: String,

  @Schema(description = "The status of the email delivery", example = "SENT")
  val emailStatus: String,

  @Schema(description = "The type of notification that was sent", example = "CREATE")
  val notificationType: String,

  @Schema(description = "The description of the notification type", example = "Visit Created")
  val notificationTypeDescription: String,
)
