package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

const val BIRMINGHAM = "BMI"
const val WANDSWORTH = "WWI"
const val PENTONVILLE = "PVI"
const val MOORLAND = "MDI"
const val SWALESIDE = "SWI"

private val nextMonday = LocalDate.now().next(DayOfWeek.MONDAY)

object Moorland {
  // These visit slots are tied to the SQL seed test data. Also, note slot dates always come after the next Monday to ensure all slot dates are contiguous. Depending on usage, the dates may need to be overridden.
  val MONDAY_9_TO_10_VISIT_SLOT = VisitSlot(
    1,
    nextMonday,
    LocalTime.of(9, 0),
    LocalTime.of(10, 0),
    moorlandLocation.id,
  )

  val WEDNESDAY_9_TO_10_VISIT_SLOT = VisitSlot(
    4,
    nextMonday.next(DayOfWeek.WEDNESDAY),
    LocalTime.of(9, 0),
    LocalTime.of(10, 0),
    moorlandLocation.id,
  )

  val FRIDAY_11_TO_12_VISIT_SLOT = VisitSlot(
    9,
    nextMonday.next(DayOfWeek.FRIDAY),
    LocalTime.of(11, 0),
    LocalTime.of(12, 0),
    moorlandLocation.id,
  )

  val VISITOR = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = CONTACT_MOORLAND_PRISONER.contactId,
    prisonerContactId = CONTACT_MOORLAND_PRISONER.prisonerContactId,
    leadVisitor = true,
    assistedVisit = true,
    visitorEquipment = VisitorEquipment("Laptop"),
    assistedNotes = "Wheelchair access needed",
  )
}

// Location UUID matches the prison_visit_slot row inserted in V2026.05.12__swaleside_test_data.sql
val SWALESIDE_LEGAL_VIDLINK_LOCATION_ID: UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479")

object Swaleside {
  // Visit slot 20 / time slot 20 – Wednesday 09:00–09:45 (maxGroups=2, maxAdults=10, maxVideoSessions=2)
  val WEDNESDAY_9_TO_9_45_VISIT_SLOT = VisitSlot(
    slotId = 20,
    date = LocalDate.now().next(DayOfWeek.WEDNESDAY),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(9, 45),
    locationId = SWALESIDE_LEGAL_VIDLINK_LOCATION_ID,
  )

  val VISITOR_1 = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 201,
    prisonerContactId = 301,
    leadVisitor = true,
    assistedVisit = false,
  )

  val VISITOR_2 = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 202,
    prisonerContactId = 302,
    leadVisitor = false,
    assistedVisit = false,
  )

  val VISITOR_3 = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 203,
    prisonerContactId = 303,
    leadVisitor = false,
    assistedVisit = false,
  )
}

