package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.extensions.isAtDifferentPrisonTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.extensions.isReleased
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.model.Prisoner
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
  private val prisonerAlertsChecker: PrisonerAlertsChecker,
) : AbstractChecker(visitReviewRepository) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun check(officialVisit: OfficialVisitEntity) {
    // Get the prisoner details - abandon the check if not found
    val prisoner = prisonerSearchClient.getPrisoner(officialVisit.prisonerNumber) ?: return

    // Identify any new issues for this visit as it stands now
    val newIssues = detectIssues(officialVisit, prisoner)

    // If there are no issues found remove previously recorded issues, as now outdated
    if (newIssues.isEmpty()) {
      visitReviewRepository.deleteByOfficialVisitId(officialVisit.officialVisitId)
      visitReviewRepository.flush()
      return
    }

    logger.info("Found ${newIssues.size} issues for ${officialVisit.officialVisitId} on ${officialVisit.visitDate} for ${officialVisit.prisonerNumber}")

    // Get the existing issue review details for this visit
    val existingReview = getExistingVisitReview(officialVisit.officialVisitId)

    // If there were was an existing review, add the new issues to it, otherwise create a new review and add them
    existingReview?.let { addIssuesToExistingReview(existingReview, newIssues) } ?: createVisitReview(officialVisit, newIssues)
  }

  /**
   * This method performs the checks to identify if any issues are present on the visit today.
   */
  private fun detectIssues(officialVisit: OfficialVisitEntity, prisoner: Prisoner): Set<IssueType> = buildSet {
    if (prisoner.isReleased()) {
      add(IssueType.PRISONER_RELEASED)
    }

    if (prisoner.isAtDifferentPrisonTo(officialVisit.prisonCode)) {
      add(IssueType.PRISONER_TRANSFERRED)
    }

    // Identify issues with visitors
    addAll(visitorIssueChecker.checkVisitorIssues(officialVisit).map { it.issueType })

    // Identify if new prisoner alerts have been created since the visit was created
    prisonerAlertsChecker.checkPrisonerAlerts(officialVisit)?.let { add(it) }
  }

  /**
   * Adds new issues to an existing review, but only if they are not already present.
   */
  private fun addIssuesToExistingReview(existingReview: VisitReviewEntity, newIssues: Set<IssueType>) {
    var issuesAdded = false
    newIssues.forEach { issueType ->
      if (existingReview.visitReviewDetails().none { it.issueType == issueType }) {
        existingReview.addVisitReviewDetails(timeSource.now(), issueType, null)
        issuesAdded = true
      }
    }

    if (issuesAdded) {
      visitReviewRepository.saveAndFlush(existingReview)
    }
  }

  /**
   * Creates a new review and adds any identified issues to it
   */
  private fun createVisitReview(officialVisit: OfficialVisitEntity, newIssues: Set<IssueType>) {
    val visitReview = VisitReviewEntity(officialVisitId = officialVisit.officialVisitId, raisedTime = timeSource.now()).apply {
      newIssues.forEach { issueType -> addVisitReviewDetails(raisedTime, issueType, null) }
    }
    visitReviewRepository.saveAndFlush(visitReview)
  }
}
