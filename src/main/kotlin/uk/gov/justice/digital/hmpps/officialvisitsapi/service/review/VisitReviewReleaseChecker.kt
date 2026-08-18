package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository

@Component
class VisitReviewReleaseChecker(
  private val visitReviewRepository: VisitReviewRepository,
  private val timeSource: TimeSource,
) : AbstractChecker(visitReviewRepository) {
  fun check(officialVisit: OfficialVisitEntity) {
    getExistingVisitReview(officialVisit.officialVisitId)?.let { existing ->
      if (existing.visitReviewDetails().none { it.issueType == IssueType.PRISONER_RELEASED }) {
        existing.addVisitReviewDetails(
          timeSource.now(),
          IssueType.PRISONER_RELEASED,
          null,
        )

        visitReviewRepository.saveAndFlush(existing)
      }

      return
    }

    visitReviewRepository.saveAndFlush(
      VisitReviewEntity(
        officialVisitId = officialVisit.officialVisitId,
        raisedTime = timeSource.now(),
      ).apply {
        addVisitReviewDetails(
          raisedTime,
          IssueType.PRISONER_RELEASED,
          null,
        )
      },
    )
  }
}
