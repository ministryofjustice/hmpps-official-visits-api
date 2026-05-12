package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.Email
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.OfficialVisitCreatedEmail
import java.util.Optional
import java.util.UUID

class NotificationsFacadeTest {
  private val notificationId = UUID.randomUUID()
  private val locationsService: LocationsService = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val emailService: EmailService = mock()
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val facade = NotificationsFacade(officialVisitRepository, locationsService, prisonerSearchClient, emailService, notificationRepository)

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

  @Test
  fun `should send create email via email service`() {
    whenever { emailService.send(any()) } doReturn Result.success(notificationId to "fake template id")

    val officialVisit = createAVisitEntity(1)
    val prisoner = prisonerSearchPrisoner(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonCode = MOORLAND_PRISONER.prison,
      bookingId = MOORLAND_PRISONER.bookingId,
    )

    whenever { officialVisitRepository.findById(1) } doReturn Optional.of(officialVisit)
    whenever { locationsService.getLocationById(officialVisit.dpsLocationId) } doReturn moorlandLocation
    whenever { prisonerSearchClient.getPrisoner(officialVisit.prisonerNumber) } doReturn prisoner

    facade.sendNotification(
      officialVisitId = 1,
      request = NotificationRequest(
        notificationType = NotificationType.CREATE,
        emailAddresses = listOf("email@address"),
      ),
      user = MOORLAND_PRISON_USER,
    )

    val emailCaptor = argumentCaptor<Email>()

    inOrder(officialVisitRepository, locationsService, prisonerSearchClient, emailService, notificationRepository) {
      verify(officialVisitRepository).findById(1)
      verify(locationsService).getLocationById(officialVisit.dpsLocationId)
      verify(prisonerSearchClient).getPrisoner(officialVisit.prisonerNumber)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(any<NotificationEntity>())
    }

    val email = emailCaptor.firstValue
    email isInstanceOf OfficialVisitCreatedEmail::class.java
    email.emailAddress isEqualTo "email@address"
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "appointment_date" to officialVisit.visitDate.toMediumFormatStyle(),
      "appointment_location" to moorlandLocation.localName,
      "appointment_time" to officialVisit.startTime.toHourMinuteStyle(),
      "prisoner_name" to prisoner.firstName + " " + prisoner.lastName,
      "user_name" to MOORLAND_PRISON_USER.name,
    )
  }

  object FakeEmail : Email("email@address") {
    override fun type() = EmailType.OFFICIAL_VISIT_CREATED
  }
}
