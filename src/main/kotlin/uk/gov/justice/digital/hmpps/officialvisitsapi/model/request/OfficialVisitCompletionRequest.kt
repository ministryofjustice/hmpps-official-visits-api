package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType

data class OfficialVisitCompletionRequest(
  @field:NotNull(message = "The completion reason is mandatory")
  val completionReason: VisitCompletionType?,

  @field:NotNull(message = "The prisoner attendance is mandatory")
  val prisonerAttendance: AttendanceType?,

  @field:NotNull(message = "The prisoner search type is mandatory")
  val prisonerSearchType: SearchLevelType?,

  @field:NotEmpty(message = "The visitors attendance is mandatory")
  val visitorAttendance: List<OfficialVisitorAttendance>,
)

data class OfficialVisitorAttendance(
  val officialVisitorId: Long,

  val prisonerAttendance: AttendanceType?,
)
