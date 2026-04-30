package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.Feature
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.NotifyTemplates
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import kotlin.reflect.full.memberProperties

@Service
class EmailNotificationService(
  @Qualifier("notificationClient") private val normalNotificationClient: NotificationClient,
  private val applicationEventPublisher: ApplicationEventPublisher,
  features: FeatureSwitches,
) : EmailNotifier {
  private val isFeatureNotificationsEnabled = features.isEnabled(Feature.FEATURE_NOTIFICATIONS_ENABLE)
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  companion object {
    fun resolveTemplateName(templateId: String) = NotifyTemplates::class.memberProperties
      .firstOrNull { it.getter.call() == templateId }?.name
  }

  val errorSuppressionList = listOf(
    // full message is 'Can`t send to this recipient using a team-only API key'. Have excluded part
    // with non-ascii characters to avoid complications
    "this recipient using a team-only API key",
    "Not a valid email address",
  )

  override fun sendEmail(
    recipientEmailAddress: String,
    templateId: String,
    personalisation: Map<String, *>,
    replyToEmailId: String?,
    reference: String?,
  ) {
    val emailRequest = EmailRequest(recipientEmailAddress, templateId, personalisation, replyToEmailId)
    applicationEventPublisher.publishEvent(SendEmailRequestedEvent(emailRequest))

    try {
      if (!isFeatureNotificationsEnabled) {
        log.info("Email sending is disabled")
        return
      }
      val templateName = resolveTemplateName(templateId)
      log.info(
        "Sending email with template $templateName ($templateId) to user ${emailRequest.email} " +
          "with replyToId ${emailRequest.replyToEmailId}. Personalisation is ${emailRequest.personalisation}",
      )
      normalNotificationClient.sendEmail(
        templateId,
        recipientEmailAddress,
        personalisation,
        reference,
        replyToEmailId,
      )
    } catch (notificationClientException: NotificationClientException) {
      val templateName = resolveTemplateName(templateId)
      log.error(
        "Unable to send template $templateName ($templateId) to user $recipientEmailAddress",
        notificationClientException,
      )

      notificationClientException.message?.let { exceptionMessage ->
        val suppress = errorSuppressionList.any { exceptionMessage.lowercase().contains(it.lowercase()) }
        if (suppress) {
          log.warn("Suppressing this error as it contains a known message indicating that retrying is unlikely to succeed: $exceptionMessage")
        } else {
          log.error("Error does not contain any known message should be investigated : $exceptionMessage")
        }
      }
    }
  }

  override fun sendEmails(
    recipientEmailAddresses: Set<String>,
    templateId: String,
    personalisation: Map<String, *>,
    replyToEmailId: String?,
    reference: String?,
  ) = recipientEmailAddresses.forEach {
    sendEmail(
      recipientEmailAddress = it,
      templateId = templateId,
      personalisation = personalisation,
      replyToEmailId = replyToEmailId,
      reference = reference,
    )
  }
}

interface EmailNotifier {
  fun sendEmail(
    recipientEmailAddress: String,
    templateId: String,
    personalisation: Map<String, *>,
    replyToEmailId: String? = null,
    reference: String? = null,
  )

  fun sendEmails(
    recipientEmailAddresses: Set<String>,
    templateId: String,
    personalisation: Map<String, *>,
    replyToEmailId: String? = null,
    reference: String? = null,
  )
}

data class EmailRequest(
  val email: String,
  val templateId: String,
  val personalisation: Map<String, *>,
  val replyToEmailId: String? = null,
)

data class SendEmailRequestedEvent(val request: EmailRequest)
