package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.NotificationType

data class NotificationResponse(
  @Schema(description = "The official visit id", example = "1")
  val officialVisitId: Long,

  @Schema(description = "The type of notification that was sent", example = "CREATE")
  val notificationType: NotificationType,

  @Schema(description = "The recipients the notification was sent to")
  val recipients: List<NotificationRecipient>,
)

data class NotificationRecipient(val emailAddress: String, val notificationId: Long)
