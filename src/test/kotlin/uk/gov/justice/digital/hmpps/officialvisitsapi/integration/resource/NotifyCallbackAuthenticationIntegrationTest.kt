package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.util.UUID

@TestPropertySource(properties = ["notify.callback.bearer-token=notify-callback-token"])
class NotifyCallbackAuthenticationIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
  }

  @Test
  fun `should reject callback when callback bearer token is missing`() {
    val notification = createNotification()

    webTestClient
      .post()
      .uri("/notify/callback")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(callbackBody(notification.govNotifyNotificationId, "delivered"))
      .exchange()
      .expectStatus().isUnauthorized

    val updated = notificationRepository.findById(notification.notificationId).orElseThrow()
    assertThat(updated.emailStatus).isEqualTo(NotificationEmailStatus.PENDING)
  }

  @Test
  fun `should reject callback when callback bearer token is invalid`() {
    val notification = createNotification()

    webTestClient
      .post()
      .uri("/notify/callback")
      .contentType(MediaType.APPLICATION_JSON)
      .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-token")
      .bodyValue(callbackBody(notification.govNotifyNotificationId, "delivered"))
      .exchange()
      .expectStatus().isUnauthorized

    val updated = notificationRepository.findById(notification.notificationId).orElseThrow()
    assertThat(updated.emailStatus).isEqualTo(NotificationEmailStatus.PENDING)
  }

  @Test
  fun `should accept callback when callback bearer token is valid`() {
    val notification = createNotification()

    webTestClient
      .post()
      .uri("/notify/callback")
      .contentType(MediaType.APPLICATION_JSON)
      .header(HttpHeaders.AUTHORIZATION, "Bearer notify-callback-token")
      .bodyValue(callbackBody(notification.govNotifyNotificationId, "delivered"))
      .exchange()
      .expectStatus().isNoContent

    val updated = notificationRepository.findById(notification.notificationId).orElseThrow()
    assertThat(updated.emailStatus).isEqualTo(NotificationEmailStatus.SENT)
  }

  private fun createNotification() = notificationRepository.saveAndFlush(
    NotificationEntity(
      officialVisitId = 1,
      templateId = "template-id",
      emailAddress = "email@address.com",
      reason = "OFFICIAL_VISIT_CREATED",
      govNotifyNotificationId = UUID.randomUUID(),
      createdTime = LocalDateTime.now(),
    ),
  )

  private fun callbackBody(notificationId: UUID, status: String) =
    """
      {
        "id": "$notificationId",
        "status": "$status",
        "created_at": "2026-05-28T09:00:00",
        "sent_at": "2026-05-28T09:05:00",
        "to": "email@address.com",
        "notification_type": "email",
        "template_id": "${UUID.randomUUID()}",
        "template_version": 1
      }
    """.trimIndent()
}
