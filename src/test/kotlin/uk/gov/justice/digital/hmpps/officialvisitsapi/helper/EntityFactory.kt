package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import java.time.LocalTime
import java.util.UUID

fun createAVisitEntity(officialVisitId: Long) = run {
  val prisonVisitSlot = PrisonVisitSlotEntity(
    prisonVisitSlotId = 1,
    prisonTimeSlotId = 1,
    dpsLocationId = UUID.randomUUID(),
    maxAdults = 1,
    maxGroups = 1,
    maxVideoSessions = 1,
    createdBy = "test-helper",
    createdTime = now(),
  )

  val visit = OfficialVisitEntity(
    officialVisitId = officialVisitId,
    prisonVisitSlot = prisonVisitSlot,
    prisonCode = PENTONVILLE,
    prisonerNumber = PENTONVILLE_PRISONER.number,
    visitDate = tomorrow(),
    startTime = LocalTime.of(11, 45),
    endTime = LocalTime.of(12, 45),
    dpsLocationId = prisonVisitSlot.dpsLocationId,
    visitTypeCode = VisitType.IN_PERSON,
    createdBy = "test-helper",
  )

  visit.addVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipTypeCode = RelationshipType.OFFICIAL,
    relationshipCode = "COM",
    contactId = 1,
    prisonerContactId = 1,
    firstName = "Community",
    lastName = "Manager",
    leadVisitor = true,
    assistedVisit = true,
    assistedNotes = "assisted notes",
    createdBy = PENTONVILLE_PRISON_USER,
    createdTime = now(),
  )

  visit.addVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipTypeCode = RelationshipType.OFFICIAL,
    relationshipCode = "POM",
    contactId = 2,
    prisonerContactId = 2,
    firstName = "Prison",
    lastName = "Manager",
    leadVisitor = false,
    assistedVisit = true,
    assistedNotes = "assisted notes",
    createdBy = PENTONVILLE_PRISON_USER,
    createdTime = now(),
  )

  visit
}

fun createAPrisonerVisitedEntity(officialVisitEntity: OfficialVisitEntity, prisonerVisitedId: Long) = PrisonerVisitedEntity(
  prisonerVisitedId = prisonerVisitedId,
  officialVisit = officialVisitEntity,
  prisonerNumber = officialVisitEntity.prisonerNumber,
  attendanceCode = AttendanceType.ATTENDED,
  createdBy = PENTONVILLE_PRISON_USER.username,
  createdTime = now(),
)
