package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import java.time.LocalDateTime

data class VisitsForReviewResponse(
  @Schema(description = "The visit details")
  val visit: OfficialVisitDetails,

  @Schema(description = "The issues requiring review for this visit")
  val issues: List<VisitForReviewIssue>,
)

data class VisitForReviewIssue(
  @Schema(description = "The visit review detail id", example = "123")
  val visitReviewDetailId: Long,

  @Schema(description = "The issue type")
  val issueType: IssueType,

  @Schema(description = "The optional issue detail")
  val issueDetail: String?,

  @Schema(description = "The time this issue was raised")
  val raisedTime: LocalDateTime,
)
