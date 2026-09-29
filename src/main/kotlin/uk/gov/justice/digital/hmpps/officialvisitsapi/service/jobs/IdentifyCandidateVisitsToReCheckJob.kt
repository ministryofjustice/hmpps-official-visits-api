package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType

/**
 * This job is responsible for re-checking visits that have already been queued for review.
 *
 * Visits will be re-checked and flagged for review again.
 * Processing is done per-prison, with each prison's visits handled in a separate transaction.
 */
@Component
class IdentifyCandidateVisitsToReCheckJob(

  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  features: FeatureSwitches,
  prisonJobProcessor: PrisonJobProcessor,
  timeSource: TimeSource,
) : PrisonAwareDailyJob<Long>(
  jobType = JobType.IDENTIFY_CANDIDATE_VISITS_TO_RECHECK,
  timeSource,
  features,
  prisonJobProcessor,
  { date, prisonCode ->
    officialVisitRepository.findCandidateVisitsForReReviewForPrison(date.plusDays(2), prisonCode)
  },
  { visitIds, _ ->
    visitIds.forEach { visitId ->
      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visitId,
          createdTime = timeSource.now(),
          triggeringEvent = VisitReviewCheckType.RECHECK,
        ),
      )
    }
  },
)
