package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.extensions.isAtDifferentPrisonTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.extensions.isReleased
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewDetailEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository

@Component
class VisitReviewChecker(
  private val visitReviewRepository: VisitReviewRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val timeSource: TimeSource,
  private val visitorIssueChecker: VisitorIssueChecker,
  private val prisonerAlertsChecker: PrisonerAlertsChecker,
) : AbstractChecker(visitReviewRepository) {

  fun check(officialVisit: OfficialVisitEntity) = processVisit(officialVisit, alreadyRaised = this::hasExistingUnAcknowledgedIssues)

  fun recheck(officialVisit: OfficialVisitEntity) = processVisit(officialVisit, alreadyRaised = this::hasExistingIssues)

  private fun processVisit(
    officialVisit: OfficialVisitEntity,
    alreadyRaised: (VisitReviewDetailEntity, IssueType) -> Boolean,
  ) {
    val prisoner = prisonerSearchClient.getPrisoner(officialVisit.prisonerNumber) ?: return
    val currentIssues = detectIssues(officialVisit, prisoner)
    if (currentIssues.isEmpty()) return

    val existingReview = getExistingVisitReview(officialVisit.officialVisitId)
    if (existingReview != null) {
      addIssuesToExistingReview(existingReview, currentIssues, alreadyRaised)
    } else {
      createVisitReview(officialVisit, currentIssues)
    }
  }

  private fun detectIssues(officialVisit: OfficialVisitEntity, prisoner: Prisoner): Set<IssueType> = buildSet {
    if (prisoner.isReleased()) add(IssueType.PRISONER_RELEASED)
    if (prisoner.isAtDifferentPrisonTo(officialVisit.prisonCode)) add(IssueType.PRISONER_TRANSFERRED)
    addAll(visitorIssueChecker.checkVisitorIssues(officialVisit).map { it.issueType })
    prisonerAlertsChecker.checkPrisonerAlerts(officialVisit)?.let { add(it) }
  }

  private fun addIssuesToExistingReview(
    existingReview: VisitReviewEntity,
    currentIssues: Set<IssueType>,
    alreadyRaised: (VisitReviewDetailEntity, IssueType) -> Boolean,
  ) {
    currentIssues.forEach { issueType ->
      if (existingReview.visitReviewDetails().none { alreadyRaised(it, issueType) }) {
        existingReview.addVisitReviewDetails(timeSource.now(), issueType, null)
        visitReviewRepository.saveAndFlush(existingReview)
      }
    }
  }

  private fun createVisitReview(officialVisit: OfficialVisitEntity, currentIssues: Set<IssueType>) {
    val visitReview = VisitReviewEntity(
      officialVisitId = officialVisit.officialVisitId,
      raisedTime = timeSource.now(),
    ).apply {
      currentIssues.forEach { issueType -> addVisitReviewDetails(raisedTime, issueType, null) }
    }

    visitReviewRepository.saveAndFlush(visitReview)
  }

  private fun hasExistingUnAcknowledgedIssues(detail: VisitReviewDetailEntity, issueType: IssueType): Boolean = detail.issueType == issueType && detail.acknowledgedBy == null

  private fun hasExistingIssues(detail: VisitReviewDetailEntity, issueType: IssueType): Boolean = detail.issueType == issueType
}
