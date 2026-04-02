package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitAuditSnapshot
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class OfficialVisitAuditDelegateTest {
  private val auditingService: AuditingService = mock()
  private val userService: UserService = mock()

  private val delegate = OfficialVisitAuditDelegate(auditingService, userService)

  @Test
  fun `recordCreated creates create audit event`() {
    val visit = createVisit()
    whenever(userService.getUser(visit.createdBy)).thenReturn(MOORLAND_PRISON_USER)

    delegate.recordCreated(visit)

    val captor = argumentCaptor<AuditEventDto>()
    verify(auditingService).recordAuditEvent(captor.capture())

    with(captor.firstValue) {
      assertThat(officialVisitId).isEqualTo(visit.officialVisitId)
      assertThat(summaryText).isEqualTo("Official visit created")
      assertThat(prisonCode).isEqualTo(MOORLAND)
      assertThat(prisonerNumber).isEqualTo(MOORLAND_PRISONER.number)
      assertThat(eventSource).isEqualTo("DPS")
    }
  }

  @Test
  fun `recordUpdated creates update audit event with field changes`() {
    val visit = createVisit()
    val snapshot = OfficialVisitAuditSnapshot.from(visit)
    whenever(userService.getUser(visit.createdBy)).thenReturn(MOORLAND_PRISON_USER)

    visit.staffNotes = "updated staff notes"
    visit.startTime = LocalTime.of(11, 0)
    visit.updatedBy = visit.createdBy

    delegate.recordUpdated(visit, snapshot)

    val captor = argumentCaptor<AuditEventDto>()
    verify(auditingService).recordAuditEvent(captor.capture())

    with(captor.firstValue) {
      assertThat(summaryText).isEqualTo("Official visit updated")
      assertThat(detailText).contains("Start time changed from 09:00 to 11:00")
      assertThat(detailText).contains("Staff notes changed from '' to updated staff notes")
    }
  }

  @Test
  fun `recordDeleted creates delete audit event`() {
    val visit = createVisit()
    whenever(userService.getUser(visit.createdBy)).thenReturn(MOORLAND_PRISON_USER)

    delegate.recordDeleted(visit, OfficialVisitAuditSnapshot.from(visit))

    val captor = argumentCaptor<AuditEventDto>()
    verify(auditingService).recordAuditEvent(captor.capture())

    with(captor.firstValue) {
      assertThat(summaryText).isEqualTo("Official visit deleted")
      assertThat(detailText).contains(MOORLAND_PRISONER.number)
      assertThat(eventSource).isEqualTo("DPS")
    }
  }

  private fun createVisit() = OfficialVisitEntity(
    officialVisitId = 101L,
    prisonVisitSlot = PrisonVisitSlotEntity(
      prisonVisitSlotId = 1L,
      prisonTimeSlotId = 1L,
      dpsLocationId = UUID.randomUUID(),
      createdBy = "TEST",
      createdTime = LocalDateTime.now().minusDays(1),
    ),
    prisonCode = MOORLAND,
    prisonerNumber = MOORLAND_PRISONER.number,
    visitDate = LocalDate.now().plusDays(10),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = UUID.randomUUID(),
    visitTypeCode = VisitType.IN_PERSON,
    createdBy = MOORLAND_PRISON_USER.username,
    createdTime = LocalDateTime.now().minusDays(1),
  )
}
