package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar

import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class SarVisit(
  val officialVisitId: Long,
  val prisonCode: String,
  val visitDate: LocalDate,
  val prisonerNumber: String,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val locationId: UUID,
  val locationName: String,
  val visitType: VisitType,
  val visitStatus: VisitStatusType,
  val completionCode: VisitCompletionType? = null,
  val completionNotes: String? = null,
  val prisonerAttendance: AttendanceType? = null,
  val staffNotes: String? = null,
  val prisonerNotes: String? = null,
  val visitors: List<SarVisitor> = emptyList(),
  val events: List<SarEvent> = emptyList(),
)

data class SarVisitor(
  val officialVisitorId: Long,
  val firstName: String?,
  val lastName: String?,
  val relationshipType: RelationshipType? = null,
  val relationshipCode: String? = null,
  val relationshipDescription: String? = null,
  val visitorNotes: String? = null,
  val visitorAttendance: AttendanceType? = null,
)

data class SarEvent(
  val auditedEventId: Long,
  val eventDateTime: LocalDateTime,
  val eventType: String,
  val eventDescription: String,
  val staffCode: String,
  val staffName: String,
)
