package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

const val BIRMINGHAM = "BMI"
const val WANDSWORTH = "WWI"
const val PENTONVILLE = "PVI"
const val MOORLAND = "MDI"

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
