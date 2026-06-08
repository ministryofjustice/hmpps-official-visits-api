package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.SentEmailsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.Email
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.OfficialVisitCancelledEmail
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.OfficialVisitCreatedEmail
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.OfficialVisitUpdatedEmail
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class NotificationsServiceTest {
  private val notificationId = UUID.randomUUID()
  private val locationsService: LocationsService = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val emailService: EmailService = mock()
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val sentEmailsService: SentEmailsService = mock()
  private val notification: NotificationEntity = mock()
  private val service = NotificationsService(officialVisitRepository, locationsService, prisonerSearchClient, emailService, notificationRepository, sentEmailsService)

  @BeforeEach
  fun beforeEach() {
    whenever { notification.notificationId } doReturn 1
    whenever { notificationRepository.saveAndFlush(any<NotificationEntity>()) } doReturn notification
  }

  @Test
  fun `should delegate to email service and save notification`() {
    whenever { emailService.send(FakeEmail) } doReturn Result.success(notificationId to "fake template id")

    service.sendOfficialVisitEmail(1L, FakeEmail)

    inOrder(emailService, notificationRepository) {
      verify(emailService).send(FakeEmail)
      verify(notificationRepository).saveAndFlush(any<NotificationEntity>())
    }
  }

  @Test
  fun `should delegate to email service but fail to save notification`() {
    whenever { emailService.send(FakeEmail) } doReturn Result.failure(RuntimeException("Bang!"))

    service.sendOfficialVisitEmail(1L, FakeEmail)

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

    service.sendNotification(
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

  @Test
  fun `should send amend email via email service`() {
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

    service.sendNotification(
      officialVisitId = 1,
      request = NotificationRequest(
        notificationType = NotificationType.AMEND,
        emailAddresses = listOf("email@address"),
      ),
      user = MOORLAND_PRISON_USER,
    )

    val emailCaptor = argumentCaptor<Email>()
    verify(emailService).send(emailCaptor.capture())

    val email = emailCaptor.firstValue
    email isInstanceOf OfficialVisitUpdatedEmail::class.java
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "appointment_date" to officialVisit.visitDate.toMediumFormatStyle(),
      "appointment_location" to moorlandLocation.localName,
      "appointment_time" to officialVisit.startTime.toHourMinuteStyle(),
      "prisoner_name" to prisoner.firstName + " " + prisoner.lastName,
      "user_name" to MOORLAND_PRISON_USER.name,
    )
  }

  @Test
  fun `should send cancel email via email service`() {
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

    service.sendNotification(
      officialVisitId = 1,
      request = NotificationRequest(
        notificationType = NotificationType.CANCEL,
        emailAddresses = listOf("email@address"),
      ),
      user = MOORLAND_PRISON_USER,
    )

    val emailCaptor = argumentCaptor<Email>()
    verify(emailService).send(emailCaptor.capture())

    val email = emailCaptor.firstValue
    email isInstanceOf OfficialVisitCancelledEmail::class.java
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "appointment_date" to officialVisit.visitDate.toMediumFormatStyle(),
      "appointment_location" to moorlandLocation.localName,
      "appointment_time" to officialVisit.startTime.toHourMinuteStyle(),
      "prisoner_name" to prisoner.firstName + " " + prisoner.lastName,
      "user_name" to MOORLAND_PRISON_USER.name,
      "visitor_names" to "Community Manager, Prison Manager",
    )
  }

  @Test
  fun `should return mapped notifications for official visit id`() {
    val createdTime = LocalDateTime.now()
    val statusUpdatedTime = createdTime.plusMinutes(10)
    val notificationEntity = NotificationEntity(
      notificationId = 123L,
      officialVisitId = 999L,
      templateId = "template-1",
      emailAddress = "email@address.com",
      reason = "OFFICIAL_VISIT_CREATED",
      govNotifyNotificationId = UUID.randomUUID(),
      emailStatus = NotificationEmailStatus.SENT,
      createdTime = createdTime,
      statusUpdatedTime = statusUpdatedTime,
    )

    whenever { notificationRepository.findByOfficialVisitIdOrderByCreatedTimeDesc(999L) } doReturn listOf(notificationEntity)

    val result = service.getNotificationsByOfficialVisitId(999L)

    result.size isEqualTo 1
    with(result.first()) {
      notificationId isEqualTo notificationEntity.notificationId
      officialVisitId isEqualTo notificationEntity.officialVisitId
      templateId isEqualTo notificationEntity.templateId
      emailAddress isEqualTo notificationEntity.emailAddress
      reason isEqualTo notificationEntity.reason
      govNotifyNotificationId isEqualTo notificationEntity.govNotifyNotificationId
      emailStatus isEqualTo notificationEntity.emailStatus
      createdTime isEqualTo notificationEntity.createdTime
      statusUpdatedTime isEqualTo notificationEntity.statusUpdatedTime
    }

    verify(notificationRepository).findByOfficialVisitIdOrderByCreatedTimeDesc(999L)
  }

  @Test
  fun `should return empty list when official visit has no notifications`() {
    whenever { notificationRepository.findByOfficialVisitIdOrderByCreatedTimeDesc(77L) } doReturn emptyList()

    val result = service.getNotificationsByOfficialVisitId(77L)

    result.isEmpty() isEqualTo true
    verify(notificationRepository).findByOfficialVisitIdOrderByCreatedTimeDesc(77L)
  }

  object FakeEmail : Email("email@address") {
    override fun type() = EmailType.OFFICIAL_VISIT_CREATED
  }
}
