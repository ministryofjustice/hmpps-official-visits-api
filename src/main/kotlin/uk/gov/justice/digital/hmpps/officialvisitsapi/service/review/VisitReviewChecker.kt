package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.extensions.isAtDifferentPrisonTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.extensions.isReleased
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository

@Component
class VisitReviewChecker(
  private val visitReviewRepository: VisitReviewRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val timeSource: TimeSource,
  private val visitorIssueChecker: VisitorIssueChecker,
) : AbstractChecker(visitReviewRepository) {
  fun check(officialVisit: OfficialVisitEntity) {
    val prisoner = prisonerSearchClient.getPrisoner(officialVisit.prisonerNumber) ?: return

    val currentIssues = buildSet {
      if (prisoner.isReleased()) add(IssueType.PRISONER_RELEASED)
      if (prisoner.isAtDifferentPrisonTo(officialVisit.prisonCode)) add(IssueType.PRISONER_TRANSFERRED)
      addAll(visitorIssueChecker.checkVisitorIssues(officialVisit).map { it.issueType })
    }

    if (currentIssues.isEmpty()) return

    getExistingVisitReview(officialVisit.officialVisitId)?.let { existing ->
      currentIssues.forEach { issueType ->
        if (existing.visitReviewDetails().none { it.issueType == issueType && it.acknowledgedBy == null }) {
          existing.addVisitReviewDetails(
            timeSource.now(),
            issueType,
            null,
          )

          visitReviewRepository.saveAndFlush(existing)
        }
      }

      return
    }

    visitReviewRepository.saveAndFlush(
      VisitReviewEntity(
        officialVisitId = officialVisit.officialVisitId,
        raisedTime = timeSource.now(),
      ).apply {
        currentIssues.forEach { issueType ->
          addVisitReviewDetails(
            raisedTime,
            issueType,
            null,
          )
        }
      },
    )
  }
}
