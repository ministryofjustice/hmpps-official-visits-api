package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.SentNotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.contains
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationSearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationSearchRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.NotificationSearchInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SentNotificationsServiceTest {
  private val notificationSearchRepository: NotificationSearchRepository = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val metricsService: MetricsService = mock()

  private val service = SentNotificationsService(notificationSearchRepository, prisonerSearchClient, metricsService)

  @Test
  fun `should search sent emails with pagination`() {
    val request = NotificationSearchRequest(
      fromDate = LocalDate.of(2026, 5, 1),
      toDate = LocalDate.of(2026, 5, 31),
    )

    val viewEntity = SentNotificationEntity(
      notificationId = 100L,
      officialVisitId = 1L,
      prisonCode = "MDI",
      sentDateTime = LocalDateTime.of(2026, 5, 22, 10, 30),
      visitDate = LocalDate.of(2026, 6, 1),
      visitStartTime = LocalTime.of(9, 0),
      visitEndTime = LocalTime.of(10, 0),
      emailAddress = "user@example.com",
      emailStatus = NotificationEmailStatus.PENDING,
      notificationType = EmailType.OFFICIAL_VISIT_CREATED.name,
      prisonerNumber = "G1234AB",
    )

    val prisoner = Prisoner(
      prisonerNumber = "G1234AB",
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1990, 1, 1),
    )

    val pageResult: Page<SentNotificationEntity> = PageImpl(
      listOf(viewEntity),
      PageRequest.of(0, 10),
      1,
    )

    whenever(
      notificationSearchRepository.findByPrisonCodeAndSentDateTimeGreaterThanEqualAndSentDateTimeLessThanOrderBySentDateTimeDesc(
        any(),
        any(),
        any(),
        any(),
      ),
    ).thenReturn(pageResult)
    whenever(prisonerSearchClient.findByPrisonerNumbers(any(), any())).thenReturn(listOf(prisoner))

    val result =
      service.searchSentNotifications(prisonCode = "MDI", request, page = 0, size = 10, user = MOORLAND_PRISON_USER)

    result.metadata.totalElements isEqualTo 1L
    result.content.size isEqualTo 1
    result.content[0].firstName isEqualTo "John"
    result.content[0].lastName isEqualTo "Smith"
    result.content[0].prisonerNumber isEqualTo "G1234AB"
    result.content[0].emailStatus isEqualTo "PENDING"
    result.content[0].notificationType isEqualTo "CREATE"
    result.content[0].notificationTypeDescription isEqualTo "Visit Created"

    val fromDateTimeCaptor = argumentCaptor<LocalDateTime>()
    val toDateTimeCaptor = argumentCaptor<LocalDateTime>()
    verify(notificationSearchRepository).findByPrisonCodeAndSentDateTimeGreaterThanEqualAndSentDateTimeLessThanOrderBySentDateTimeDesc(
      eq("MDI"),
      fromDateTimeCaptor.capture(),
      toDateTimeCaptor.capture(),
      any(),
    )
    fromDateTimeCaptor.firstValue isEqualTo LocalDateTime.of(2026, 5, 1, 0, 0)
    toDateTimeCaptor.firstValue isEqualTo LocalDateTime.of(2026, 6, 1, 0, 0)
    verify(metricsService).send(
      eventType = eq(MetricsEvents.NOTIFICATION_SEARCH),
      info = eq(
        NotificationSearchInfo(
          prisonCode = "MDI",
          username = MOORLAND_PRISON_USER.username,
          fromDate = LocalDate.of(2026, 5, 1),
          toDate = LocalDate.of(2026, 5, 31),
          numberOfResults = 1,
        ),
      ),
    )
  }

  @Test
  fun `should throw exception when page number is negative`() {
    val request = NotificationSearchRequest(fromDate = null, toDate = null)

    assertThrows<IllegalArgumentException> {
      service.searchSentNotifications(prisonCode = "MDI", request, page = -1, size = 10, user = MOORLAND_PRISON_USER)
    }.message?.contains("Page number must be greater than or equal to zero")
  }

  @Test
  fun `should throw exception when page size is zero`() {
    val request = NotificationSearchRequest(fromDate = null, toDate = null)

    assertThrows<IllegalArgumentException> {
      service.searchSentNotifications(prisonCode = "MDI", request, page = 0, size = 0, user = MOORLAND_PRISON_USER)
    }.message?.contains("Page size must be greater than zero")
  }

  @Test
  fun `should throw exception when prison code is blank`() {
    val request = NotificationSearchRequest(fromDate = null, toDate = null)

    assertThrows<IllegalArgumentException> {
      service.searchSentNotifications(prisonCode = " ", request, page = 0, size = 10, user = MOORLAND_PRISON_USER)
    }.message?.contains("Prison code must be provided")
  }

  @Test
  fun `should search without date filters`() {
    val request = NotificationSearchRequest(fromDate = null, toDate = null)

    whenever(notificationSearchRepository.findByPrisonCodeOrderBySentDateTimeDesc(any(), any()))
      .thenReturn(PageImpl(emptyList(), PageRequest.of(0, 10), 0))

    val result =
      service.searchSentNotifications(prisonCode = "MDI", request, page = 0, size = 10, user = MOORLAND_PRISON_USER)

    result.content.size isEqualTo 0
    result.metadata.totalElements isEqualTo 0L
    verify(notificationSearchRepository).findByPrisonCodeOrderBySentDateTimeDesc(eq("MDI"), any())
  }

  @Test
  fun `should throw exception when from date is after to date`() {
    val request = NotificationSearchRequest(
      fromDate = LocalDate.of(2026, 5, 31),
      toDate = LocalDate.of(2026, 5, 1),
    )

    assertThrows<IllegalArgumentException> {
      service.searchSentNotifications(prisonCode = "MDI", request, page = 0, size = 10, user = MOORLAND_PRISON_USER)
    }.message?.contains("From date must be on or before to date")
  }

  @Test
  fun `should map updated and cancelled notification types`() {
    val request = NotificationSearchRequest(fromDate = null, toDate = null)

    val updatedEntity = SentNotificationEntity(
      notificationId = 200L,
      officialVisitId = 2L,
      prisonCode = "MDI",
      sentDateTime = LocalDateTime.of(2026, 5, 23, 9, 30),
      visitDate = LocalDate.of(2026, 6, 2),
      visitStartTime = LocalTime.of(14, 0),
      visitEndTime = LocalTime.of(15, 0),
      emailAddress = "updated@example.com",
      emailStatus = NotificationEmailStatus.SENT,
      notificationType = EmailType.OFFICIAL_VISIT_UPDATED.name,
      prisonerNumber = "G1234AB",
    )

    val cancelledEntity = SentNotificationEntity(
      notificationId = 300L,
      officialVisitId = 3L,
      prisonCode = "MDI",
      sentDateTime = LocalDateTime.of(2026, 5, 24, 11, 0),
      visitDate = LocalDate.of(2026, 6, 3),
      visitStartTime = LocalTime.of(10, 30),
      visitEndTime = LocalTime.of(11, 30),
      emailAddress = "cancelled@example.com",
      emailStatus = NotificationEmailStatus.PERMANENT_FAILURE,
      notificationType = EmailType.OFFICIAL_VISIT_CANCELLED.name,
      prisonerNumber = "G5678CD",
    )

    whenever(notificationSearchRepository.findByPrisonCodeOrderBySentDateTimeDesc(any(), any()))
      .thenReturn(PageImpl(listOf(updatedEntity, cancelledEntity), PageRequest.of(0, 10), 2))

    whenever(prisonerSearchClient.findByPrisonerNumbers(any(), any())).thenReturn(
      listOf(
        Prisoner(
          prisonerNumber = "G1234AB",
          firstName = "John",
          lastName = "Smith",
          dateOfBirth = LocalDate.of(1990, 1, 1),
        ),
        Prisoner(
          prisonerNumber = "G5678CD",
          firstName = "Jane",
          lastName = "Taylor",
          dateOfBirth = LocalDate.of(1988, 8, 8),
        ),
      ),
    )

    val result =
      service.searchSentNotifications(prisonCode = "MDI", request, page = 0, size = 10, user = MOORLAND_PRISON_USER)

    result.content[0].notificationType isEqualTo "UPDATED"
    result.content[0].notificationTypeDescription isEqualTo "Visit Updated"
    result.content[1].notificationType isEqualTo "CANCELLED"
    result.content[1].notificationTypeDescription isEqualTo "Visit Cancelled"
  }
}
