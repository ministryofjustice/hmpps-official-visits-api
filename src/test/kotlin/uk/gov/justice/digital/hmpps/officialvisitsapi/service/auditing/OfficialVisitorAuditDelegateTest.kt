package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class OfficialVisitorAuditDelegateTest {
  private val auditingService: AuditingService = mock()
  private val userService: UserService = mock()

  private val delegate = OfficialVisitorAuditDelegate(auditingService, userService)

  @Test
  fun `recordCreated creates visitor added audit event`() {
    val visit = createVisit()
    val visitor = OfficialVisitorEntity(
      officialVisitorId = 201L,
      officialVisit = visit,
      visitorTypeCode = VisitorType.CONTACT,
      firstName = "jane",
      lastName = "doe",
      contactId = 3001L,
      prisonerContactId = 4001L,
      relationshipTypeCode = RelationshipType.OFFICIAL,
      relationshipCode = "POM",
      createdBy = MOORLAND_PRISON_USER.username,
      createdTime = LocalDateTime.now().minusHours(1),
    )
    whenever(userService.getUser(visitor.createdBy)).thenReturn(MOORLAND_PRISON_USER)

    delegate.recordCreated(visitor)

    val captor = argumentCaptor<AuditEventDto>()
    verify(auditingService).recordAuditEvent(captor.capture())

    with(captor.firstValue) {
      assertThat(officialVisitId).isEqualTo(visit.officialVisitId)
      assertThat(summaryText).isEqualTo("Official visitor added")
      assertThat(detailText).isEqualTo("Official visitor Jane Doe added to visit for prisoner number ${MOORLAND_PRISONER.number}")
      assertThat(prisonCode).isEqualTo(MOORLAND)
      assertThat(prisonerNumber).isEqualTo(MOORLAND_PRISONER.number)
      assertThat(username).isEqualTo(MOORLAND_PRISON_USER.username)
      assertThat(userFullName).isEqualTo(MOORLAND_PRISON_USER.name)
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
