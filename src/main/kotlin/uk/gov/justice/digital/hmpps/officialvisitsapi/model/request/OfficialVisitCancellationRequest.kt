package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType

data class OfficialVisitCancellationRequest(
  @Schema(
    description = "The reason for cancellation of the visit",
    example = "PRISONER_CANCELLED",
    allowableValues = ["PRISONER_CANCELLED", "STAFF_CANCELLED", "VISITOR_CANCELLED"],
  )
  @field:NotNull(message = "The cancellation reason is mandatory")
  val cancellationReason: VisitCompletionType?,

  @Schema(description = "Optional notes containing details of the reason for cancellation", example = "Prisoner in hospital")
  val cancellationNotes: String? = null,
) {
  @JsonIgnore
  @AssertTrue(message = "The cancellation reason is not valid or allowed")
  private fun isInvalidCancellationReason() = cancellationReason == null || cancellationReason.isCancellation
}
