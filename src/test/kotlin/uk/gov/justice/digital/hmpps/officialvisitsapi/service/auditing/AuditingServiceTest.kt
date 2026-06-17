package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
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
  private val visitId = 3L

  @BeforeEach
  fun beforeEach() {
    whenever { officialVisitRepository.findById(any()) } doReturn Optional.of(officialVisitEntity)
  }

  @Test
  fun `should map audited create event correctly`() {
    val createdEvent = event(101, "Visit created", "")

    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(createdEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 101L
      officialVisitId isEqualTo visitId
      eventType isEqualTo "CREATE"
      eventSummary isEqualTo "Visit created"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo createdEvent.eventDateTime
      eventUsername isEqualTo createdEvent.userName
      eventUserFullName isEqualTo createdEvent.userFullName
      significantChange isBool false
      eventChanges hasSize 0
      eventVersion isEqualTo 2
    }
  }

  @Test
  fun `should map audited update event with significant changes correctly`() {
    val updatedEvent = event(
      auditEventId = 101,
      summaryText = "Visit updated",
      detailText = "visit_date|oldValue1|newValue1;start_time|oldValue2|newValue2;end_time|oldValue3|newValue3;location|oldValue4|newValue4;random_field|oldValue5|newValue5;visit_status|oldValue6|newValue6;",
    )

    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(updatedEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 101L
      officialVisitId isEqualTo visitId
      eventType isEqualTo "UPDATE"
      eventSummary isEqualTo "Visit updated"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo updatedEvent.eventDateTime
      eventUsername isEqualTo updatedEvent.userName
      eventUserFullName isEqualTo updatedEvent.userFullName
      significantChange isBool true
      eventChanges.containsExactly(
        listOf(
          AuditedEventChange(field = "visit_date", oldValue = "oldValue1", newValue = "newValue1", true),
          AuditedEventChange(field = "start_time", oldValue = "oldValue2", newValue = "newValue2", true),
          AuditedEventChange(field = "end_time", oldValue = "oldValue3", newValue = "newValue3", true),
          AuditedEventChange(field = "location", oldValue = "oldValue4", newValue = "newValue4", true),
          AuditedEventChange(field = "random_field", oldValue = "oldValue5", newValue = "newValue5", false),
          AuditedEventChange(field = "visit_status", oldValue = "oldValue6", newValue = "newValue6", true),
        ),
      )
      eventVersion isEqualTo 2
    }
  }

  @Test
  fun `should map audited update event without significant changes correctly`() {
    val updatedEvent = event(
      auditEventId = 101,
      summaryText = "Visit updated",
      detailText = "insignificant_field|oldValue|newValue;",
    )

    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(updatedEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 101L
      officialVisitId isEqualTo visitId
      eventType isEqualTo "UPDATE"
      eventSummary isEqualTo "Visit updated"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo updatedEvent.eventDateTime
      eventUsername isEqualTo updatedEvent.userName
      eventUserFullName isEqualTo updatedEvent.userFullName
      significantChange isBool false
      eventChanges.containsExactly(
        listOf(
          AuditedEventChange(field = "insignificant_field", oldValue = "oldValue", newValue = "newValue", false),
        ),
      )
      eventVersion isEqualTo 2
    }
  }

  @Test
  fun `should map audited cancelled event correctly`() {
    val cancelledEvent = event(auditEventId = 101, summaryText = "Visit cancelled", detailText = "")

    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(cancelledEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 101L
      officialVisitId isEqualTo visitId
      eventType isEqualTo "CANCELLED"
      eventSummary isEqualTo "Visit cancelled"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo cancelledEvent.eventDateTime
      eventUsername isEqualTo cancelledEvent.userName
      eventUserFullName isEqualTo cancelledEvent.userFullName
      significantChange isBool false
      eventChanges hasSize 0
      eventVersion isEqualTo 2
    }
  }

  @Test
  fun `should map audited completed event correctly`() {
    val completedEvent = event(auditEventId = 101, summaryText = "Visit completed", detailText = "")

    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(completedEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 101L
      officialVisitId isEqualTo visitId
      eventType isEqualTo "COMPLETED"
      eventSummary isEqualTo "Visit completed"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo completedEvent.eventDateTime
      eventUsername isEqualTo completedEvent.userName
      eventUserFullName isEqualTo completedEvent.userFullName
      significantChange isBool false
      eventChanges hasSize 0
      eventVersion isEqualTo 2
    }
  }

  @Test
  fun `should map audited prisoner merged event correctly`() {
    val completedEvent = event(auditEventId = 101, summaryText = "Prisoner merged", detailText = "prisoner_number|oldValue|newValue;")

    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(completedEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 101L
      officialVisitId isEqualTo visitId
      eventType isEqualTo "UPDATE"
      eventSummary isEqualTo "Prisoner merged"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo completedEvent.eventDateTime
      eventUsername isEqualTo completedEvent.userName
      eventUserFullName isEqualTo completedEvent.userFullName
      significantChange isBool false
      eventChanges.containsExactly(
        listOf(
          AuditedEventChange(field = "prisoner_number", oldValue = "oldValue", newValue = "newValue", false),
        ),
      )
      eventVersion isEqualTo 2
    }
  }

  @Test
  fun `should map visitor change event`() {
    val visitorChangedEvent = event(auditEventId = 101, summaryText = "Visitor changed", detailText = "visitor_added||newValue;visitor_updated||newValue;visitor_removed||newValue;")

    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(visitorChangedEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 101L
      officialVisitId isEqualTo visitId
      eventType isEqualTo "UPDATE"
      eventSummary isEqualTo "Visitor changed"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo visitorChangedEvent.eventDateTime
      eventUsername isEqualTo visitorChangedEvent.userName
      eventUserFullName isEqualTo visitorChangedEvent.userFullName
      significantChange isBool false
      eventChanges.containsExactly(
        listOf(
          AuditedEventChange(field = "visitor_added", oldValue = null, newValue = "newValue", false),
          AuditedEventChange(field = "visitor_updated", oldValue = null, newValue = "newValue", false),
          AuditedEventChange(field = "visitor_removed", oldValue = null, newValue = "newValue", false),
        ),
      )
      eventVersion isEqualTo 2
    }
  }

  @Test
  fun `should only include staff facing events`() {
    val staffFacingEvent = event(
      auditEventId = 100,
      summaryText = "Visitor changed",
      detailText = "visitor_added||newValue;visitor_updated||newValue;visitor_removed||newValue;",
    )

    val nonStaffFacingEvents = AuditEventType.entries.filterNot { it.isStaffFacing }.mapIndexed { index, type ->
      event(index.toLong(), type.summaryText, "")
    }

    whenever { officialVisitRepository.findById(visitId) } doReturn Optional.of(officialVisitEntity)
    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(staffFacingEvent) + nonStaffFacingEvents

    auditingService.findByOfficialVisitId(visitId).single().auditedEventId isEqualTo 100
  }

  @Test
  fun `should map legacy audit events`() {
    val legacyAuditedEvent = AuditedEventEntity(
      auditedEventId = 100,
      officialVisitId = visitId,
      eventSource = "DPS",
      userName = "X123Y",
      userFullName = "Jane Doe",
      summaryText = "Visitor changed",
      detailText = "Some random detail text",
      eventDateTime = LocalDateTime.now(),
      prisonCode = MOORLAND,
      prisonerNumber = MOORLAND_PRISONER.number,
      versionNumber = null,
    )

    whenever(auditedEventRepository.findAllByOfficialVisitId(visitId)) doReturn listOf(legacyAuditedEvent)

    val event = auditingService.findByOfficialVisitId(visitId).single()

    with(event) {
      auditedEventId isEqualTo 100
      officialVisitId isEqualTo visitId
      eventType isEqualTo "OTHER"
      eventSummary isEqualTo "Visitor changed"
      eventDetail isEqualTo "Some random detail text"
      eventSource isEqualTo "DPS"
      eventDateTime isEqualTo legacyAuditedEvent.eventDateTime
      eventUsername isEqualTo legacyAuditedEvent.userName
      eventUserFullName isEqualTo legacyAuditedEvent.userFullName
      significantChange isBool false
      eventChanges hasSize 0
      eventVersion isEqualTo 1
    }
  }

  private fun event(auditEventId: Long = 101L, summaryText: String, detailText: String) = AuditedEventEntity(
    auditedEventId = auditEventId,
    officialVisitId = visitId,
    eventSource = "DPS",
    userName = "X123Y",
    userFullName = "Jane Doe",
    summaryText = summaryText,
    detailText = detailText,
    eventDateTime = LocalDateTime.now(),
    prisonCode = MOORLAND,
    prisonerNumber = MOORLAND_PRISONER.number,
    versionNumber = 2,
  )
}
