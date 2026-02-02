package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType

class OfficialVisitCancellationRequestTest : ValidatorBase<OfficialVisitCancellationRequest>() {
  private val request = OfficialVisitCancellationRequest(
    cancellationReason = VisitCompletionType.VISITOR_CANCELLED,
    cancellationNotes = "Visitor cancelled due to unforeseen circumstances",
  )

  @Test
  fun `should be no errors for valid requests`() {
    VisitCompletionType.entries.filter { it.isCancellation }.forEach { reason ->
      assertNoErrors(request.copy(cancellationReason = reason))
    }
  }

  @Test
  fun `should be errors for invalid requests`() {
    request.copy(cancellationReason = null) failsWithSingle ModelError("cancellationReason", "The cancellation reason is mandatory")

    VisitCompletionType.entries.filterNot { it.isCancellation }.forEach { reason ->
      request.copy(cancellationReason = reason) failsWithSingle ModelError("invalidCancellationReason", "The cancellation reason is not valid or allowed")
    }
  }
}
