package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailTemplate
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailTemplates
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailType
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
  @Value($$"${notify.templates.in-person-visit.confirmed:}") private val inPersonVisitConfirmedTemplateId: String,
  @Value($$"${notify.templates.in-person-visit.amended:}") private val inPersonVisitAmendedTemplateId: String,
  @Value($$"${notify.templates.in-person-visit.cancelled:}") private val inPersonVisitCancelledTemplateId: String,
  @Value($$"${notify.templates.telephone-visit.confirmed:}") private val telephoneVisitConfirmedTemplateId: String,
  @Value($$"${notify.templates.telephone-visit.amended:}") private val telephoneVisitAmendedTemplateId: String,
  @Value($$"${notify.templates.telephone-visit.cancelled:}") private val telephoneVisitCancelledTemplateId: String,
  @Value($$"${notify.templates.video-visit.confirmed:}") private val videoVisitConfirmedTemplateId: String,
  @Value($$"${notify.templates.video-visit.amended:}") private val videoVisitAmendedTemplateId: String,
  @Value($$"${notify.templates.video-visit.cancelled:}") private val videoVisitCancelledTemplateId: String,
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
    setOf(
      EmailTemplate(officialVisitCreatedTemplateId, OFFICIAL_VISIT_CREATED),
      EmailTemplate(officialVisitCancelledTemplateId, OFFICIAL_VISIT_CANCELLED),
      EmailTemplate(officialVisitUpdatedTemplateId, OFFICIAL_VISIT_UPDATED),
      EmailTemplate(inPersonVisitConfirmedTemplateId, EmailType.IN_PERSON_VISIT_CONFIRMED),
      EmailTemplate(inPersonVisitAmendedTemplateId, EmailType.IN_PERSON_VISIT_AMENDED),
      EmailTemplate(inPersonVisitCancelledTemplateId, EmailType.IN_PERSON_VISIT_CANCELLED),
      EmailTemplate(telephoneVisitConfirmedTemplateId, EmailType.TELEPHONE_VISIT_CONFIRMED),
      EmailTemplate(telephoneVisitAmendedTemplateId, EmailType.TELEPHONE_VISIT_AMENDED),
      EmailTemplate(telephoneVisitCancelledTemplateId, EmailType.TELEPHONE_VISIT_CANCELLED),
      EmailTemplate(videoVisitConfirmedTemplateId, EmailType.VIDEO_VISIT_CONFIRMED),
      EmailTemplate(videoVisitAmendedTemplateId, EmailType.VIDEO_VISIT_AMENDED),
      EmailTemplate(videoVisitCancelledTemplateId, EmailType.VIDEO_VISIT_CANCELLED),
    ),
  )
}
