package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.Email
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailService
import java.time.LocalDateTime

@Component
class NotificationsFacade(
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun sendOfficialVisitEmail(officialVisitId: Long, email: Email) {
    emailService.send(email)
      .onSuccess { (notificationId, templateId) ->
        notificationRepository.saveAndFlush(
          NotificationEntity(
            officialVisitId = officialVisitId,
            templateId = templateId,
            emailAddress = email.emailAddress,
            reason = email.type().name,
            govNotifyNotificationId = notificationId,
            createdTime = LocalDateTime.now(),
          ),
        )
      }
      .onFailure { exception -> logger.info("Failed to send email ${email.type()}.", exception) }
  }
}
