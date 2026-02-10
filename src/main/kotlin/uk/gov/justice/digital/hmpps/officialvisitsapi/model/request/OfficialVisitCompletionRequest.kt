package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType

data class OfficialVisitCompletionRequest(
  @Schema(
    description = "The reason for completion of the visit",
    example = "NORMAL",
    allowableValues = ["NORMAL", "PRISONER_EARLY", "PRISONER_REFUSED", "STAFF_EARLY", "VISITOR_DENIED", "VISITOR_EARLY", "VISITOR_NO_SHOW"],
  )
  @field:NotNull(message = "The completion reason is mandatory")
  val completionReason: VisitCompletionType?,

  @Schema(description = "Optional notes containing details of the completion", example = "The visitor was late arriving so the the prisoner may need another visit to be arranged.")
  val completionNotes: String? = null,

  @field:NotNull(message = "The prisoner attendance is mandatory")
  val prisonerAttendance: AttendanceType?,

  @field:NotNull(message = "The prisoner search type is mandatory")
  val prisonerSearchType: SearchLevelType?,

  @field:NotEmpty(message = "The visitors attendance is mandatory")
  val visitorAttendance: List<OfficialVisitorAttendance>,
) {
  @JsonIgnore
  @AssertTrue(message = "The completion reason is not valid or allowed")
  private fun isInvalidCompletionReason() = completionReason == null || !completionReason.isCancellation
}

data class OfficialVisitorAttendance(
  val officialVisitorId: Long,

  val visitorAttendance: AttendanceType?,
)
