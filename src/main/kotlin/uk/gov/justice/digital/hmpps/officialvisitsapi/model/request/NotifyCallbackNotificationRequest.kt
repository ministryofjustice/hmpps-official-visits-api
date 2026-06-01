package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Gov Notify Callback Notification")
data class NotifyCallbackNotificationRequest(
  @param:Schema(description = "The UUID of the notification", required = true)
  @param:JsonAlias("id")
  val notificationId: UUID,
  @param:Schema(description = "The id of the event audit which the notification is linked to", required = true)
  @param:JsonAlias("reference")
  val eventAuditReference: String?,
  @param:Schema(description = "The final status of the notification", required = true)
  val status: String,
  @param:Schema(description = "The timestamp for when the vsip notification service sent the notification to gov notify", required = true)
  @param:JsonAlias("created_at")
  val createdAt: LocalDateTime,
  @param:Schema(description = "The timestamp for the final update of the notification (when delivered or ultimately failed) ", required = false)
  @param:JsonAlias("completed_at")
  val completedAt: LocalDateTime?,
  @param:Schema(description = "The timestamp for when gov notify sent the notification", required = false)
  @param:JsonAlias("sent_at")
  val sentAt: LocalDateTime?,
  @param:Schema(description = "The email or phone number the notification was sent to", required = true)
  @param:JsonAlias("to")
  val sentTo: String,
  @param:Schema(description = "The type of the notification", required = true)
  @param:JsonAlias("notification_type")
  val notificationType: String,
  @param:Schema(description = "The id the template used for the notification", required = true)
  @param:JsonAlias("template_id")
  val templateId: UUID,
  @param:Schema(description = "The version of the template used for the notification", required = true)
  @param:JsonAlias("template_version")
  val templateVersion: Int,
)
