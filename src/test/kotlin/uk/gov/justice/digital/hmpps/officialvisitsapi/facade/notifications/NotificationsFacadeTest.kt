package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.Email
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailType
import java.util.UUID

class NotificationsFacadeTest {
  private val notificationId = UUID.randomUUID()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val facade = NotificationsFacade(emailService, notificationRepository)

  @Test
  fun `should delegate to email service and save notification`() {
    whenever { emailService.send(FakeEmail) } doReturn Result.success(notificationId to "fake template id")

    facade.sendOfficialVisitEmail(1L, FakeEmail)

    inOrder(emailService, notificationRepository) {
      verify(emailService).send(FakeEmail)
      verify(notificationRepository).saveAndFlush(any<NotificationEntity>())
    }
  }

  @Test
  fun `should delegate to email service but fail to save notification`() {
    whenever { emailService.send(FakeEmail) } doReturn Result.failure(RuntimeException("Bang!"))

    facade.sendOfficialVisitEmail(1L, FakeEmail)

    verify(emailService).send(FakeEmail)
    verifyNoInteractions(notificationRepository)
  }

  object FakeEmail : Email("email@address") {
    override fun type() = EmailType.PLACEHOLDER_EMAIL_TYPE
  }
}
