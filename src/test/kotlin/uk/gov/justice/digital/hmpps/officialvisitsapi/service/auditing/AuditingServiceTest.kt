package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AuditedEventEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isBool
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AuditedEventChange
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AuditedEventRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import java.time.LocalDateTime
import java.util.Optional

class AuditingServiceTest {
  private val officialVisitEntity: OfficialVisitEntity = mock()
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val auditedEventRepository: AuditedEventRepository = mock()
  private val auditingService = AuditingService(officialVisitRepository, auditedEventRepository)

  @Test
  fun `should map audited create event with significant changes correctly`() {
    val visitId = 3L

    val auditedEvent = AuditedEventEntity(
      auditedEventId = 101L,
      officialVisitId = visitId,
      eventSource = "DPS",
      userName = "X123Y",
      userFullName = "Jane Doe",
      summaryText = "Visit created",
      detailText = "",
      eventDateTime = LocalDateTime.now(),
      prisonCode = MOORLAND,
      prisonerNumber = MOORLAND_PRISONER.number,
    )

    whenever { officialVisitRepository.findById(visitId) } doReturn Optional.of(officialVisitEntity)
    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(auditedEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 101L
      officialVisitId isEqualTo visitId
      eventType isEqualTo "CREATE"
      eventSummary isEqualTo "Visit created"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo auditedEvent.eventDateTime
      eventUsername isEqualTo auditedEvent.userName
      eventUserFullName isEqualTo auditedEvent.userFullName
      significantChange isBool false
      eventChanges hasSize 0
    }
  }

  @Test
  fun `should map audited update event with significant changes correctly`() {
    val visitId = 3L

    val auditedEvent = AuditedEventEntity(
      auditedEventId = 101L,
      officialVisitId = visitId,
      eventSource = "DPS",
      userName = "X123Y",
      userFullName = "Jane Doe",
      summaryText = "Visit updated",
      detailText = "visit_date|oldValue1|newValue1;start_time|oldValue2|newValue2;end_time|oldValue3|newValue3;location|oldValue4|newValue4;random_field|oldValue5|newValue5;",
      eventDateTime = LocalDateTime.now(),
      prisonCode = MOORLAND,
      prisonerNumber = MOORLAND_PRISONER.number,
    )

    whenever { officialVisitRepository.findById(visitId) } doReturn Optional.of(officialVisitEntity)
    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(auditedEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 101L
      officialVisitId isEqualTo visitId
      eventType isEqualTo "UPDATE"
      eventSummary isEqualTo "Visit updated"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo auditedEvent.eventDateTime
      eventUsername isEqualTo auditedEvent.userName
      eventUserFullName isEqualTo auditedEvent.userFullName
      significantChange isBool true
      eventChanges.containsExactly(
        listOf(
          AuditedEventChange(field = "visit_date", oldValue = "oldValue1", newValue = "newValue1", true),
          AuditedEventChange(field = "start_time", oldValue = "oldValue2", newValue = "newValue2", true),
          AuditedEventChange(field = "end_time", oldValue = "oldValue3", newValue = "newValue3", true),
          AuditedEventChange(field = "location", oldValue = "oldValue4", newValue = "newValue4", true),
          AuditedEventChange(field = "random_field", oldValue = "oldValue5", newValue = "newValue5", false),
        ),
      )
    }
  }
}
