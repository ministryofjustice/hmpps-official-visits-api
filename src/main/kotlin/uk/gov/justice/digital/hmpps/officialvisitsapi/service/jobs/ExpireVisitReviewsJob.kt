package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType.EXPIRE_VISIT_REVIEWS
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService
import java.time.LocalDate

/**
 * This job is responsible for expiring any visit reviews when a visit has passed its scheduled date and time.
 */
@Component
class ExpireVisitReviewsJob(
  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewService: VisitReviewService,
  private val featureSwitches: FeatureSwitches,
  private val transactionalPrisonJobProcessor: TransactionalPrisonJobProcessor,
) : VisitReviewJob<Long>(
  jobType = EXPIRE_VISIT_REVIEWS,
  featureSwitches,
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
