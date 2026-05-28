package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotifyCallbackRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

class NotifyCallbackServiceTest {
  private val repository: NotificationRepository = mock()

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
    val completedAt = OffsetDateTime.parse("2026-05-28T10:15:00Z")

    whenever(repository.findByGovNotifyNotificationId(notificationId)) doReturn notification

    val facade = NotifyCallbackService(repository, "")
    facade.processCallback(NotifyCallbackRequest(id = notificationId, status = "delivered", completedAt = completedAt), null)

    verify(repository).findByGovNotifyNotificationId(notificationId)
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.SENT)
    assertThat(notification.statusUpdatedTime).isEqualTo(completedAt.toLocalDateTime())
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
    val service = NotifyCallbackService(repository, "")

    service.processCallback(NotifyCallbackRequest(id = notificationId, status = "permanent-failure"), null)
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.PERMANENT_FAILURE)

    service.processCallback(NotifyCallbackRequest(id = notificationId, status = "temporary-failure"), null)
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.TEMPORARY_FAILURE)

    service.processCallback(NotifyCallbackRequest(id = notificationId, status = "technical-failure"), null)
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.TECHNICAL_FAILURE)

    service.processCallback(NotifyCallbackRequest(id = notificationId, status = "unexpected-status"), null)
    assertThat(notification.emailStatus).isEqualTo(NotificationEmailStatus.UNKNOWN)
  }

  @Test
  fun `should reject callback when bearer token is invalid`() {
    val facade = NotifyCallbackService(repository, "expected-token")

    assertThrows<ResponseStatusException> {
      facade.processCallback(NotifyCallbackRequest(id = UUID.randomUUID(), status = "delivered"), "Bearer wrong-token")
    }

    verifyNoInteractions(repository)
  }
}
