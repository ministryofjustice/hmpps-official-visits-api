package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailTemplate
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailTemplates
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailType.OFFICIAL_VISIT_CANCELLED
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailType.OFFICIAL_VISIT_CREATED
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailType.OFFICIAL_VISIT_UPDATED
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.GovNotifyEmailService
import uk.gov.service.notify.NotificationClient
import java.util.UUID

@Configuration
class GovNotifyConfiguration(
  @Value($$"${notify.api.key:}") private val apiKey: String,
  @Value($$"${notify.templates.official-visit-created:}") private val officialVisitCreatedTemplateId: String,
  @Value($$"${notify.templates.official-visit-cancelled:}") private val officialVisitCancelledTemplateId: String,
  @Value($$"${notify.templates.official-visit-updated:}") private val officialVisitUpdatedTemplateId: String,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun emailService() = run {
    if (apiKey.isBlank()) {
      EmailService { _ -> Result.success(UUID.randomUUID() to "fake_template_id") }.also { logger.info("Gov Notify notifications are disabled") }
    } else {
      GovNotifyEmailService(
        NotificationClient(apiKey),
        emailTemplates(),
      ).also { logger.info("Gov Notify notifications are enabled") }
    }
  }

  @Bean
  fun emailTemplates() = EmailTemplates(
    setOfNotNull(
      officialVisitCreatedTemplateId.takeIf { it.isNotBlank() }
        ?.let { EmailTemplate(it, OFFICIAL_VISIT_CREATED) },
      officialVisitCancelledTemplateId.takeIf { it.isNotBlank() }
        ?.let { EmailTemplate(it, OFFICIAL_VISIT_CANCELLED) },
      officialVisitUpdatedTemplateId.takeIf { it.isNotBlank() }
        ?.let { EmailTemplate(it, OFFICIAL_VISIT_UPDATED) },
    ),
  )
}
