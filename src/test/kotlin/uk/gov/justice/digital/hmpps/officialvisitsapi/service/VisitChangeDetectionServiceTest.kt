package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AuditedEventEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isBool
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AuditedEventRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import java.time.LocalDateTime
import java.util.UUID

class VisitChangeDetectionServiceTest {

  private val auditedEventRepository: AuditedEventRepository = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val service = VisitChangeDetectionService(auditedEventRepository, notificationRepository)

  private val officialVisitId = 123L
  private val createdTime = LocalDateTime.of(2026, 6, 3, 10, 0)
  private val topOnePage = PageRequest.of(0, 1)
  private val notification = notification(createdTime = createdTime)

  @BeforeEach
  fun beforeEach() {
    whenever { notificationRepository.findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId) } doReturn notification
  }

  @Test
  fun `should return false when no notification exists`() {
    whenever { notificationRepository.findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId) } doReturn null

    val result = service.hasVisitDetailsChanged(officialVisitId)

    result isBool false
    verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
  }

  @Test
  fun `should return false when there are no relevant audit events after notification`() {
    whenever { auditedEventRepository.findRelevantAuditEventsAfter(officialVisitId, createdTime, topOnePage) } doReturn emptyList()

    val result = service.hasVisitDetailsChanged(officialVisitId)

    result isBool false
    verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
    verify(auditedEventRepository).findRelevantAuditEventsAfter(officialVisitId, createdTime, topOnePage)
  }

  @Test
  fun `should return true when a relevant audit event exists after notification`() {
    whenever { auditedEventRepository.findRelevantAuditEventsAfter(officialVisitId, createdTime, topOnePage) } doReturn listOf(
      cancelledAuditEvent(eventDateTime = createdTime.plusMinutes(1)),
    )

    val result = service.hasVisitDetailsChanged(officialVisitId)

    result isBool true
    verify(notificationRepository).findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
    verify(auditedEventRepository).findRelevantAuditEventsAfter(officialVisitId, createdTime, topOnePage)
  }

  private fun notification(createdTime: LocalDateTime) = NotificationEntity(
    officialVisitId = officialVisitId,
    templateId = "template-id",
    emailAddress = "test@example.com",
    reason = "OFFICIAL_VISIT_CREATED",
    govNotifyNotificationId = UUID.randomUUID(),
    createdTime = createdTime,
  )

  private fun cancelledAuditEvent(eventDateTime: LocalDateTime) = AuditedEventEntity(
    officialVisitId = officialVisitId,
    prisonCode = "MDI",
    prisonerNumber = "A1234BC",
    eventSource = "DPS",
    userName = "STAFF1",
    userFullName = "Staff Member",
    summaryText = "Official visit cancelled",
    detailText = "Visit cancelled by user Staff Member.",
    eventDateTime = eventDateTime,
  )
}
