package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import java.time.LocalDateTime
import java.util.UUID

data class OfficialVisitNotification(
  @Schema(description = "The notification id", example = "1")
  val notificationId: Long,
  @Schema(description = "The official visit id", example = "1")
  val officialVisitId: Long,
  @Schema(description = "The template id", example = "1")
  val templateId: String,
  @Schema(description = "The email address", example = "exmple@example.com")
  val emailAddress: String,
  @Schema(description = "The reason", example = "the reason")
  val reason: String,
  @Schema(description = "The gov notify notification id", example = "12345678-1234-1234-1234-123456789012")
  val govNotifyNotificationId: UUID,
  @Schema(description = "The email status", example = "SENT")
  val emailStatus: NotificationEmailStatus,
  @Schema(description = "The created time", example = "09:00")
  val createdTime: LocalDateTime,
  @Schema(description = "The status updated time", example = "10:00")
  val statusUpdatedTime: LocalDateTime?,
)
