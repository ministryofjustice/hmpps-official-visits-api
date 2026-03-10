package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

fun createOfficialVisitRequest(visitSlot: VisitSlot, visitors: List<OfficialVisitor>) = CreateOfficialVisitRequest(
  prisonerNumber = MOORLAND_PRISONER.number,
  prisonVisitSlotId = visitSlot.slotId,
  visitDate = visitSlot.date,
  startTime = visitSlot.startTime,
  endTime = visitSlot.endTime,
  dpsLocationId = visitSlot.locationId,
  visitTypeCode = VisitType.IN_PERSON,
  staffNotes = "private notes",
  prisonerNotes = "public notes",
  searchTypeCode = SearchLevelType.PAT,
  officialVisitors = visitors,
)

data class VisitSlot(val slotId: Long, val date: LocalDate, val startTime: LocalTime, val endTime: LocalTime, val locationId: UUID)
