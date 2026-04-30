package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

// TODO calls to save notifications should be made here in the facade
@Component
class NotificationsFacade(private val emailService: EmailService) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendEmail(email: Email) {
    emailService.send(email)
      // TODO save notification details when email is sent successfully
      .onSuccess { logger.info("Sent email ${email.type}") }
      .onFailure { exception -> logger.info("Failed to send email ${email.type}.", exception) }
  }
}
