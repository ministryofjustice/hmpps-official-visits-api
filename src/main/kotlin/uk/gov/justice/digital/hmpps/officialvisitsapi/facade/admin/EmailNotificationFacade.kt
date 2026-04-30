package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.SendTestEmailRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.EmailNotificationService

@Component
class EmailNotificationFacade(private val emailNotificationService: EmailNotificationService) {
  fun sendTestEmail(request: SendTestEmailRequest) {
    emailNotificationService.sendEmail(
      recipientEmailAddress = request.recipientEmailAddress,
      templateId = request.templateId,
      personalisation = request.personalisation,
      replyToEmailId = request.replyToEmailId,
      reference = request.reference,
    )
  }
}
