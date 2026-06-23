package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isBool
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AuditedEventChange
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AuditedEventResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
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
  private val sentNotificationsService: SentNotificationsService = mock()
  private val auditingService: AuditingService = mock()
  private val notification: NotificationEntity = mock()

  private val service = NotificationsService(
    officialVisitRepository,
    locationsService,
    prisonerSearchClient,
    emailService,
    notificationRepository,
    sentNotificationsService,
    auditingService,
  )

  object FakeEmail : Email("email@address") {
    override fun type() = EmailType.OFFICIAL_VISIT_CREATED
  }

  @Nested
  inner class SendingNotifications {
    @BeforeEach
    fun beforeEach() {
      reset(notificationRepository, officialVisitRepository, emailService, prisonerSearchClient)

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
  }

  @Nested
  inner class RetrievingNotifications {
    @BeforeEach
    fun beforeEach() {
      reset(notificationRepository, officialVisitRepository)
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
      whenever { officialVisitRepository.findById(999L) } doReturn Optional.of(createAVisitEntity(999L))
      whenever {
        notificationRepository.findByOfficialVisitId(999L, sort = Sort.by(Sort.Direction.DESC, "createdTime"))
      } doReturn listOf(notificationEntity)

      val result = service.getNotificationsByOfficialVisitId(999L, sort = Sort.by(Sort.Direction.DESC, "createdTime"))

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

      verify(notificationRepository).findByOfficialVisitId(999L, sort = Sort.by(Sort.Direction.DESC, "createdTime"))
    }

    @Test
    fun `should return an empty list when the official visit has no notifications`() {
      whenever { officialVisitRepository.findById(77L) } doReturn Optional.of(createAVisitEntity(77L))
      whenever {
        notificationRepository.findByOfficialVisitId(77L, sort = Sort.by(Sort.Direction.DESC, "createdTime"))
      } doReturn emptyList()

      val result = service.getNotificationsByOfficialVisitId(77L, sort = Sort.by(Sort.Direction.DESC, "createdTime"))

      result.isEmpty() isEqualTo true
      verify(notificationRepository).findByOfficialVisitId(77L, sort = Sort.by(Sort.Direction.DESC, "createdTime"))
    }

    @Test
    fun `should throw not found exception when the official does not exist`() {
      val officialVisitId = 77L
      whenever { officialVisitRepository.findById(officialVisitId) } doReturn Optional.empty()
      whenever {
        notificationRepository.findByOfficialVisitId(officialVisitId, sort = Sort.by(Sort.Direction.DESC, "createdTime"))
      } doReturn emptyList()

      assertThrows<EntityNotFoundException> {
        service.getNotificationsByOfficialVisitId(officialVisitId, sort = Sort.by(Sort.Direction.DESC, "createdTime"))
      }.message isEqualTo "Official visit with id $officialVisitId not found"
    }
  }

  @Nested
  inner class ChangedSinceLastNotification {
    private val officialVisitId = 123L
    private val createdTime = LocalDateTime.of(2026, 6, 3, 10, 0)
    private val notification = notification(createdTime)

    private fun notification(createdTime: LocalDateTime) = NotificationEntity(
      officialVisitId = officialVisitId,
      templateId = "template-id",
      emailAddress = "test@example.com",
      reason = "OFFICIAL_VISIT_CREATED",
      govNotifyNotificationId = UUID.randomUUID(),
      createdTime = createdTime,
    )

    private fun auditedEventV2(eventDateTime: LocalDateTime) = AuditedEventResponse(
      auditedEventId = 1L,
      officialVisitId = officialVisitId,
      eventSource = "DPS",
      eventSummary = "Visit cancelled",
      eventDetail = "Visit cancelled",
      eventType = "CANCELLED",
      eventUsername = "STAFF1",
      eventUserFullName = "Staff Member",
      eventChanges = emptyList(),
      eventDateTime = eventDateTime,
      eventVersion = 2,
    )

    private fun auditedEventV1(eventDateTime: LocalDateTime) = AuditedEventResponse(
      auditedEventId = 1L,
      officialVisitId = officialVisitId,
      eventSource = "DPS",
      eventSummary = "Visit changed",
      eventDetail = "Visit changed",
      eventType = "OTHER",
      eventUsername = "STAFF1",
      eventUserFullName = "Staff Member",
      eventDateTime = eventDateTime,
      eventVersion = 1,
    )

    @BeforeEach
    fun beforeEach() {
      reset(notificationRepository, auditingService)
      whenever { notificationRepository.findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId) } doReturn notification
    }

    @Test
    fun `should return true when no notifications exist for the visit`() {
      whenever { notificationRepository.findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId) } doReturn null

      val result = service.checkVisitChangedSinceLastNotification(officialVisitId)

      result.hasChanged isBool true
      verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
      verifyNoInteractions(auditingService)
    }

    @Test
    fun `should return false when there are no audited changes since the last notification`() {
      whenever { auditingService.findByOfficialVisitId(officialVisitId) } doReturn emptyList()

      val result = service.checkVisitChangedSinceLastNotification(officialVisitId)

      result.hasChanged isBool false

      verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
      verify(auditingService).findByOfficialVisitId(officialVisitId)
    }

    @Test
    fun `should return false when audited changes since the last notification were not significant`() {
      whenever { auditingService.findByOfficialVisitId(officialVisitId) } doReturn listOf(
        auditedEventV2(LocalDateTime.now()).copy(
          eventType = "UPDATED",
          eventDetail = "prisoner_notes|null|some notes",
          eventChanges = listOf(
            AuditedEventChange(
              field = "prisoner_notes",
              oldValue = null,
              newValue = "some notes",
              significantChange = false,
            ),
          ),
        ),
      )

      val result = service.checkVisitChangedSinceLastNotification(officialVisitId)

      result.hasChanged isBool false

      verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
      verify(auditingService).findByOfficialVisitId(officialVisitId)
    }

    @Test
    fun `should return true when any audited changes since the last notification are considered significant`() {
      whenever { auditingService.findByOfficialVisitId(officialVisitId) } doReturn listOf(
        auditedEventV2(LocalDateTime.now()).copy(
          eventType = "UPDATED",
          eventDetail = "start_time|10:00|11:00",
          eventChanges = listOf(
            AuditedEventChange(
              field = "start_time",
              oldValue = "10:00",
              newValue = "11:00",
              significantChange = true,
            ),
          ),
        ),
      )

      val result = service.checkVisitChangedSinceLastNotification(officialVisitId)

      result.hasChanged isBool true

      verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
      verify(auditingService).findByOfficialVisitId(officialVisitId)
    }

    @Test
    fun `should return true when both significant and insignificant changes are present since the last notification`() {
      whenever { auditingService.findByOfficialVisitId(officialVisitId) } doReturn listOf(
        auditedEventV2(LocalDateTime.now()).copy(
          eventType = "UPDATED",
          eventDetail = "start_time|10:00|11:00",
          eventChanges = listOf(
            AuditedEventChange(
              field = "start_time",
              oldValue = "10:00",
              newValue = "11:00",
              significantChange = true,
            ),
          ),
        ),
        auditedEventV2(LocalDateTime.now()).copy(
          eventType = "UPDATED",
          eventDetail = "prisoner_notes|null|notes",
          eventChanges = listOf(
            AuditedEventChange(
              field = "prisoner_notes",
              oldValue = null,
              newValue = "notes",
              significantChange = false,
            ),
          ),
        ),
      )

      val result = service.checkVisitChangedSinceLastNotification(officialVisitId)

      result.hasChanged isBool true

      verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
      verify(auditingService).findByOfficialVisitId(officialVisitId)
    }

    @Test
    fun `should return true when a version 2 event indicates a cancellation since the last notification`() {
      whenever { auditingService.findByOfficialVisitId(officialVisitId) } doReturn listOf(
        auditedEventV2(LocalDateTime.now()).copy(
          eventType = "CANCELLED",
          eventDetail = "visit_status|SCHEDULED|CANCELLED",
          eventChanges = listOf(
            AuditedEventChange(
              field = "visit_status",
              oldValue = "SCHEDULED",
              newValue = "CANCELLED",
              significantChange = true,
            ),
          ),
        ),
      )

      val result = service.checkVisitChangedSinceLastNotification(officialVisitId)

      result.hasChanged isBool true

      verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
      verify(auditingService).findByOfficialVisitId(officialVisitId)
    }

    @Test
    fun `should return false when only audited events version 1 are present since the last notification`() {
      whenever { auditingService.findByOfficialVisitId(officialVisitId) } doReturn listOf(auditedEventV1(LocalDateTime.now()))

      val result = service.checkVisitChangedSinceLastNotification(officialVisitId)

      result.hasChanged isBool false

      verify(auditingService).findByOfficialVisitId(officialVisitId)
      verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
      verify(auditingService).findByOfficialVisitId(officialVisitId)
    }
  }
}
