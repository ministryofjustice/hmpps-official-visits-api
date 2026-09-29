package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType.EXPIRE_VISITS_FOR_REVIEW
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

/**
 * This job is responsible for expiring the visits that need to be reviewed.
 *
 * Visits for review will be expired.
 * Processing is done per-prison, with each prison's visits handled in a separate transaction.
 */
@Component
class ExpireVisitsForReviewJob(
  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewService: VisitReviewService,
  features: FeatureSwitches,
  prisonJobProcessor: PrisonJobProcessor,
  timeSource: TimeSource,
) : PrisonAwareDailyJob<Long>(
  jobType = EXPIRE_VISITS_FOR_REVIEW,
  timeSource,
  features,
  prisonJobProcessor,
  { date, prisonCode ->
    officialVisitRepository.findOverdueVisitsWithUnacknowledgedReviewDetailsBeforeForPrison(date, prisonCode)
  },
  { visitIds, _ ->
    visitIds.forEach { visitId ->
      visitReviewService.expire(visitId)
    }
  },
)
