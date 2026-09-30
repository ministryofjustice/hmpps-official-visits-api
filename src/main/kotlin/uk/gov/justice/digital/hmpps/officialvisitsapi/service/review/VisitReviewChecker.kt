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
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun check(officialVisit: OfficialVisitEntity) = processVisit(officialVisit, alreadyRaised = this::hasExistingUnAcknowledgedIssues)

  fun recheck(officialVisit: OfficialVisitEntity) = processVisit(officialVisit, alreadyRaised = this::hasExistingIssues)

  private fun processVisit(
    officialVisit: OfficialVisitEntity,
    alreadyRaised: (VisitReviewDetailEntity, IssueType) -> Boolean,
  ) {
    val prisoner = prisonerSearchClient.getPrisoner(officialVisit.prisonerNumber) ?: return
    val currentIssues = detectIssues(officialVisit, prisoner)
    if (currentIssues.isEmpty()) {
      logger.info("processVisit: No issues detected for ${officialVisit.officialVisitId} on ${officialVisit.visitDate} for ${officialVisit.prisonerNumber}")
      return
    }

    logger.info("processVisit: Found ${currentIssues.size} issues for ${officialVisit.officialVisitId} on ${officialVisit.visitDate} for ${officialVisit.prisonerNumber}")

    val existingReview = getExistingVisitReview(officialVisit.officialVisitId)
    if (existingReview != null) {
      addIssuesToExistingReview(existingReview, currentIssues, alreadyRaised)
    } else {
      logger.info("processVisit: This is the first list of issues for ${officialVisit.officialVisitId} on ${officialVisit.visitDate} for ${officialVisit.prisonerNumber}")
      createVisitReview(officialVisit, currentIssues)
    }
  }

  private fun detectIssues(officialVisit: OfficialVisitEntity, prisoner: Prisoner): Set<IssueType> = buildSet {
    if (prisoner.isReleased()) {
      logger.info("detectIssues: Prisoner is released - adding PRISONER_RELEASED issue for ${officialVisit.officialVisitId} on ${officialVisit.visitDate} for ${officialVisit.prisonerNumber}")
      add(IssueType.PRISONER_RELEASED)
    }

    if (prisoner.isAtDifferentPrisonTo(officialVisit.prisonCode)) {
      logger.info("detectIssues: Prisoner is transferred - adding PRISONER_TRANSFERRED issue for ${officialVisit.officialVisitId} on ${officialVisit.visitDate} for ${officialVisit.prisonerNumber}")
      add(IssueType.PRISONER_TRANSFERRED)
    }

    logger.info("detectIssues: Checking for visitor issues for ${officialVisit.officialVisitId} on ${officialVisit.visitDate} for ${officialVisit.prisonerNumber}")
    addAll(visitorIssueChecker.checkVisitorIssues(officialVisit).map { it.issueType })

    logger.info("detectIssues: Checking for new prisoner alerts for ${officialVisit.officialVisitId} on ${officialVisit.visitDate} for ${officialVisit.prisonerNumber}")
    prisonerAlertsChecker.checkPrisonerAlerts(officialVisit)?.let { add(it) }
  }

  private fun addIssuesToExistingReview(
    existingReview: VisitReviewEntity,
    currentIssues: Set<IssueType>,
    alreadyRaised: (VisitReviewDetailEntity, IssueType) -> Boolean,
  ) {
    logger.info("addIssuesToExistingReview: There are already existing issues for ${existingReview.officialVisitId} - adding more")
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

    logger.info("createVisitReview: Adding ${currentIssues.size} issues to a new review for ${officialVisit.officialVisitId}")

    visitReviewRepository.saveAndFlush(visitReview)
  }

  private fun hasExistingUnAcknowledgedIssues(detail: VisitReviewDetailEntity, issueType: IssueType): Boolean = detail.issueType == issueType && detail.acknowledgedBy == null

  private fun hasExistingIssues(detail: VisitReviewDetailEntity, issueType: IssueType): Boolean = detail.issueType == issueType
}
