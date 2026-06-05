package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotifyCallbackNotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import java.time.LocalDateTime
import java.util.UUID

class NotifyCallbackServiceTest {
  private val repository: NotificationRepository = mock()

  private val sharedSecret = "TestValue"

  @Test
  fun `should update notification status when callback exists`() {
    val notificationId = UUID.randomUUID()
    val notification = NotificationEntity(
      officialVisitId = 1,
      templateId = "template-id",
      emailAddress = "test@example.com",
      reason = "OFFICIAL_VISIT_CREATED",
      govNotifyNotificationId = notificationId,
      createdTime = LocalDateTime.now(),
    )
    val completedAt = LocalDateTime.parse("2026-05-28T10:15:00")

    whenever(repository.findByGovNotifyNotificationId(notificationId)) doReturn notification

    val service = NotifyCallbackService(repository, sharedSecret)

    service.processCallback(callbackRequest(notificationId, "delivered", completedAt), "Bearer $sharedSecret")

    verify(repository).findByGovNotifyNotificationId(notificationId)
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.SENT)
    assertThat(notification.statusUpdatedTime).isEqualTo(completedAt)
  }

  @Test
  fun `should map all notify statuses to internal enum`() {
    val notificationId = UUID.randomUUID()
    val notification = NotificationEntity(
      officialVisitId = 1,
      templateId = "template-id",
      emailAddress = "test@example.com",
      reason = "OFFICIAL_VISIT_CREATED",
      govNotifyNotificationId = notificationId,
      createdTime = LocalDateTime.now(),
    )

    whenever(repository.findByGovNotifyNotificationId(notificationId)) doReturn notification

    val service = NotifyCallbackService(repository, sharedSecret)

    service.processCallback(callbackRequest(notificationId, "permanent-failure"), "Bearer $sharedSecret")
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.PERMANENT_FAILURE)

    service.processCallback(callbackRequest(notificationId, "temporary-failure"), "Bearer $sharedSecret")
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.TEMPORARY_FAILURE)

    service.processCallback(callbackRequest(notificationId, "technical-failure"), "Bearer $sharedSecret")
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.TECHNICAL_FAILURE)

    service.processCallback(callbackRequest(notificationId, "unexpected-status"), "Bearer $sharedSecret")
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.UNKNOWN)
  }

  private fun callbackRequest(
    notificationId: UUID,
    status: String,
    completedAt: LocalDateTime? = null,
  ) = NotifyCallbackNotificationRequest(
    notificationId = notificationId,
    eventAuditReference = "event-audit-reference",
    status = status,
    createdAt = LocalDateTime.parse("2026-05-28T09:00:00"),
    completedAt = completedAt,
    sentAt = LocalDateTime.parse("2026-05-28T09:05:00"),
    sentTo = "test@example.com",
    notificationType = "email",
    templateId = UUID.randomUUID(),
    templateVersion = 1,
  )
}
