package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.Feature
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

class EmailNotificationServiceTest {
  private val notificationClient: NotificationClient = mock()
  private val applicationEventPublisher: ApplicationEventPublisher = mock()
  private val featureSwitches: FeatureSwitches = mock()

  @Test
  fun `sendEmail publishes event and does not call notify when feature is disabled`() {
    whenever(featureSwitches.isEnabled(Feature.FEATURE_NOTIFICATIONS_ENABLE)).thenReturn(false)

    val service = EmailNotificationService(notificationClient, applicationEventPublisher, featureSwitches)

    service.sendEmail(
      recipientEmailAddress = "recipient@example.com",
      templateId = "template-id",
      personalisation = mapOf("name" to "Recipient"),
      replyToEmailId = "reply-to-id",
      reference = "test-reference",
    )

    val eventCaptor = argumentCaptor<SendEmailRequestedEvent>()
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture())
    assertThat(eventCaptor.firstValue.request.email).isEqualTo("recipient@example.com")
    assertThat(eventCaptor.firstValue.request.templateId).isEqualTo("template-id")
    verifyNoInteractions(notificationClient)
  }

  @Test
  fun `sendEmail calls notify client when feature is enabled`() {
    whenever(featureSwitches.isEnabled(Feature.FEATURE_NOTIFICATIONS_ENABLE)).thenReturn(true)

    val service = EmailNotificationService(notificationClient, applicationEventPublisher, featureSwitches)
    val personalisation = mapOf("name" to "Recipient")

    service.sendEmail(
      recipientEmailAddress = "recipient@example.com",
      templateId = "template-id",
      personalisation = personalisation,
      replyToEmailId = "reply-to-id",
      reference = "test-reference",
    )

    verify(notificationClient).sendEmail(
      eq("template-id"),
      eq("recipient@example.com"),
      eq(personalisation),
      eq("test-reference"),
      eq("reply-to-id"),
    )
  }

  @Test
  fun `sendEmail suppresses notification client exception`() {
    whenever(featureSwitches.isEnabled(Feature.FEATURE_NOTIFICATIONS_ENABLE)).thenReturn(true)
    val exception = mock<NotificationClientException>()
    whenever(exception.message).thenReturn("Not a valid email address")
    whenever(notificationClient.sendEmail(any(), any(), any(), any(), any())).thenThrow(exception)

    val service = EmailNotificationService(notificationClient, applicationEventPublisher, featureSwitches)

    service.sendEmail(
      recipientEmailAddress = "invalid-email",
      templateId = "template-id",
      personalisation = emptyMap<String, String>(),
      reference = "test-reference",
    )

    verify(notificationClient).sendEmail(
      eq("template-id"),
      eq("invalid-email"),
      eq(emptyMap<String, String>()),
      eq("test-reference"),
      eq(null),
    )
  }

  @Test
  fun `sendEmails sends one email per recipient`() {
    whenever(featureSwitches.isEnabled(Feature.FEATURE_NOTIFICATIONS_ENABLE)).thenReturn(true)

    val service = EmailNotificationService(notificationClient, applicationEventPublisher, featureSwitches)

    service.sendEmails(
      recipientEmailAddresses = setOf("one@example.com", "two@example.com"),
      templateId = "template-id",
      personalisation = mapOf("name" to "Recipient"),
      reference = "test-reference",
    )

    verify(notificationClient, times(2)).sendEmail(
      eq("template-id"),
      any(),
      eq(mapOf("name" to "Recipient")),
      eq("test-reference"),
      eq(null),
    )
    verify(applicationEventPublisher, times(2)).publishEvent(any<SendEmailRequestedEvent>())
  }
}
