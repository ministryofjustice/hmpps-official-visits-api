package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.util.UUID

class NotifyCallbackIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
  }

  @Test
  fun `should accept callback without oauth token and update notification status`() {
    val govNotifyId = UUID.randomUUID()
    val notification = notificationRepository.saveAndFlush(
      NotificationEntity(
        officialVisitId = 1,
        templateId = "template-id",
        emailAddress = "email@address.com",
        reason = "OFFICIAL_VISIT_CREATED",
        govNotifyNotificationId = govNotifyId,
        createdTime = LocalDateTime.now(),
      ),
    )

    val completedAt = "2026-05-28T13:45:00Z"

    webTestClient
      .post()
      .uri("/notify/callback")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        """
          {
            "id": "$govNotifyId",
            "status": "delivered",
            "completed_at": "$completedAt",
            "notification_type": "email"
          }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isNoContent

    val updated = notificationRepository.findById(notification.notificationId).orElseThrow()
    assertThat(updated.emailStatus).isEqualTo(NotificationEmailStatus.SENT)
    assertThat(updated.statusUpdatedTime).isEqualTo(LocalDateTime.parse("2026-05-28T13:45:00"))
  }

  @Test
  fun `should map permanent failure callback status`() {
    val govNotifyId = UUID.randomUUID()
    val notification = notificationRepository.saveAndFlush(
      NotificationEntity(
        officialVisitId = 1,
        templateId = "template-id",
        emailAddress = "email@address.com",
        reason = "OFFICIAL_VISIT_CREATED",
        govNotifyNotificationId = govNotifyId,
        createdTime = LocalDateTime.now(),
      ),
    )

    webTestClient
      .post()
      .uri("/notify/callback")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        """
          {
            "id": "$govNotifyId",
            "status": "permanent-failure",
            "notification_type": "email"
          }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isNoContent

    val updated = notificationRepository.findById(notification.notificationId).orElseThrow()
    assertThat(updated.emailStatus).isEqualTo(NotificationEmailStatus.PERMANENT_FAILURE)
    assertThat(updated.statusUpdatedTime).isNotNull
  }
}
