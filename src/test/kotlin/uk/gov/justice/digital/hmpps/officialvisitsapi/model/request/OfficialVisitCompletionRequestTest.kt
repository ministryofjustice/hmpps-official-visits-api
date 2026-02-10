package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType

class OfficialVisitCompletionRequestTest : ValidatorBase<OfficialVisitCompletionRequest>() {
  private val request = OfficialVisitCompletionRequest(
    completionReason = VisitCompletionType.VISITOR_EARLY,
    completionNotes = "a".repeat(240),
    prisonerAttendance = AttendanceType.ATTENDED,
    prisonerSearchType = SearchLevelType.FULL,
    visitorAttendance = listOf(
      OfficialVisitorAttendance(
        1,
        AttendanceType.ATTENDED,
      ),
    ),
  )

  @Test
  fun `should be no errors for valid requests`() {
    VisitCompletionType.entries.filterNot { it.isCancellation }.forEach { reason ->
      assertNoErrors(request.copy(completionReason = reason))
    }
  }

  @Test
  fun `should be errors for in valid requests`() {
    request.copy(completionReason = null) failsWithSingle ModelError("completionReason", "The completion reason is mandatory")
    request.copy(prisonerAttendance = null) failsWithSingle ModelError("prisonerAttendance", "The prisoner attendance is mandatory")
    request.copy(completionNotes = "a".repeat(241)) failsWithSingle ModelError("completionNotes", "The completion notes should not exceed 240 characters")

    VisitCompletionType.entries.filter { it.isCancellation }.forEach { reason ->
      request.copy(completionReason = reason) failsWithSingle ModelError("invalidCompletionReason", "The completion reason is not valid or allowed")
    }
  }
}
