package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import uk.gov.service.notify.NotificationClient

class GovNotifyEmailService(private val client: NotificationClient, private val emailTemplates: EmailTemplates) : EmailService {
  override fun send(email: Email): Result<Pair<NotificationId, TemplateId>> = runCatching {
    val templateId = emailTemplates.templateIdFor(email.type)
      ?: throw RuntimeException("missing template ID for email ${email.type}.")

    client.sendEmail(
      templateId,
      email.emailAddress,
      email.personalisation(),
      null,
    ).notificationId to templateId
  }
}
