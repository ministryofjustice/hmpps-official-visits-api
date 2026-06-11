package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isBool
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.SendEmailResponse
import java.util.UUID

class GovNotifyEmailServiceTest {
  private val sendEmailResponse = Mockito.mock<SendEmailResponse>()
  private val notificationId = UUID.randomUUID()
  private val notificationClient = Mockito.mock<NotificationClient>()
  private val service = GovNotifyEmailService(
    notificationClient,
    EmailTemplates(
      setOf(EmailTemplate("template_id", EmailType.OFFICIAL_VISIT_CREATED)),
    ),
  )

  @Test
  fun `should succeed to send of email`() {
    whenever { notificationClient.sendEmail(any(), any(), any(), anyOrNull()) } doReturn sendEmailResponse
    whenever { sendEmailResponse.notificationId } doReturn notificationId

    service.send(FakeEmail).isSuccess isBool true
  }

  @Test
  fun `should fail to send of email`() {
    whenever { notificationClient.sendEmail(any(), any(), any(), anyOrNull()) } doThrow RuntimeException("Bang!")

    service.send(FakeEmail).isFailure isBool true
  }

  object FakeEmail : Email("email@address") {
    override fun type() = EmailType.OFFICIAL_VISIT_CREATED
  }
}
