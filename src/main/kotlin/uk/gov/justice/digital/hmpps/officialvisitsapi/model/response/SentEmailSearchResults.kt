package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class SentEmailRecord(
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

  @Schema(description = "The prisoner's full name", example = "John Smith")
  val prisonerName: String,

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

data class SentEmailSearchResults(
  @Schema(description = "The page content")
  val content: List<SentEmailRecord>,
  @Schema(description = "Paging metadata for the current response")
  val page: SentEmailSearchPage,
)

data class SentEmailSearchPage(
  @Schema(description = "The size of the current page", example = "20")
  val size: Long,
  @Schema(description = "The current page number (zero-based)", example = "0")
  val number: Long,
  @Schema(description = "The total number of elements", example = "150")
  val totalElements: Long,
  @Schema(description = "The total number of pages", example = "8")
  val totalPages: Long,
)
