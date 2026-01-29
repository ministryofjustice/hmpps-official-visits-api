package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import java.time.LocalTime
import java.util.UUID

class OfficialVisitEntityTest {
  @Test
  fun `should complete scheduled official visit`() {
    val visit = scheduledFutureVisit()

    visit.visitStatusCode isEqualTo VisitStatusType.SCHEDULED
    visit.searchTypeCode isEqualTo null
    visit.completionCode isEqualTo null
    visit.updatedBy isEqualTo null
    visit.updatedTime isEqualTo null
    visit.officialVisitors().single().attendanceCode isEqualTo null

    visit.complete(
      completionCode = VisitCompletionType.VISITOR_EARLY,
      prisonerSearchType = SearchLevelType.FULL,
      visitorAttendance = mapOf(0L to AttendanceType.ATTENDED),
      completedBy = MOORLAND_PRISON_USER,
    )

    visit.visitStatusCode isEqualTo VisitStatusType.COMPLETED
    visit.searchTypeCode isEqualTo SearchLevelType.FULL
    visit.completionCode isEqualTo VisitCompletionType.VISITOR_EARLY
    visit.updatedBy isEqualTo MOORLAND_PRISON_USER.username
    visit.updatedTime isCloseTo now()
    visit.officialVisitors().single().attendanceCode isEqualTo AttendanceType.ATTENDED
  }

  @Test
  fun `should reject completion of completed official visit`() {
    val completedVisit = scheduledFutureVisit()
      .complete(
        completionCode = VisitCompletionType.VISITOR_EARLY,
        prisonerSearchType = SearchLevelType.FULL,
        visitorAttendance = emptyMap(),
        completedBy = MOORLAND_PRISON_USER,
      )

    val exception = assertThrows<IllegalArgumentException> {
      completedVisit.complete(
        completionCode = VisitCompletionType.VISITOR_EARLY,
        prisonerSearchType = SearchLevelType.FULL,
        visitorAttendance = emptyMap(),
        completedBy = MOORLAND_PRISON_USER,
      )
    }

    exception.message isEqualTo "Only scheduled or expired visits can be completed."
  }

  private fun scheduledFutureVisit() = run {
    val prisonVisitSlot = PrisonVisitSlotEntity(
      prisonVisitSlotId = 1,
      prisonTimeSlotId = 1,
      dpsLocationId = UUID.randomUUID(),
      maxAdults = 1,
      maxGroups = 1,
      maxVideoSessions = 1,
      createdBy = "unit test",
      createdTime = now(),
    )

    val visit = OfficialVisitEntity(
      prisonVisitSlot = prisonVisitSlot,
      prisonCode = PENTONVILLE,
      prisonerNumber = PENTONVILLE_PRISONER.number,
      visitDate = tomorrow(),
      startTime = LocalTime.of(11, 45),
      endTime = LocalTime.of(12, 45),
      dpsLocationId = prisonVisitSlot.dpsLocationId,
      visitTypeCode = VisitType.IN_PERSON,
      createdBy = "unit test",
    )

    visit.addVisitor(
      visitorTypeCode = VisitorType.CONTACT,
      relationshipTypeCode = RelationshipType.OFFICIAL,
      relationshipCode = "relationshop code",
      contactId = 1,
      prisonerContactId = 1,
      firstName = "first name",
      lastName = "last name",
      leadVisitor = true,
      assistedVisit = false,
      createdBy = MOORLAND_PRISON_USER,
      createdTime = now(),
    )

    visit
  }
}
