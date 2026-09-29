package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType.EXPIRE_VISITS_FOR_REVIEW
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService
import java.time.LocalDate

/**
 * This job is responsible for expiring the visits that have passed their scheduled date and time.
 *
 *
 *
 * Visits for review will be expired.
 * Processing is done per-prison, with each prison's visits handled in a separate transaction.
 */
@Component
class ExpireVisitsForReviewJob(
  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewService: VisitReviewService,
  features: FeatureSwitches,
  transactionalPrisonJobProcessor: TransactionalPrisonJobProcessor,
) : PrisonAwareDailyJob<Long>(
  jobType = EXPIRE_VISITS_FOR_REVIEW,
  features,
  transactionalPrisonJobProcessor,
  { prisonCode ->
    officialVisitRepository.findOverdueVisitsWithUnacknowledgedReviewDetailsBeforeForPrison(LocalDate.now(), prisonCode)
  },
  { visitIds, _ ->
    visitIds.forEach { visitId ->
      visitReviewService.expire(visitId)
    }
  },
)
