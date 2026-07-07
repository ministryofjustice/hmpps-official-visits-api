package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar

import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalTime

data class SarVisit(
  val prisonCode: String,
  val visitDate: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val visitType: VisitType,
  val visitStatus: VisitStatusType,
  val completionCode: VisitCompletionType? = null,
  val completionNotes: String? = null,
  val prisonerAttendance: AttendanceType? = null,
  val staffNotes: String? = null,
  val prisonerNotes: String? = null,
  val searchTypeCode: String? = null,
  val visitorConcernNotes: String? = null,
  val visitors: List<SarVisitor> = emptyList(),
)

data class SarVisitor(
  val relationshipType: RelationshipType? = null,
  val relationshipCode: String? = null,
  val visitorAttendance: AttendanceType? = null,
)
