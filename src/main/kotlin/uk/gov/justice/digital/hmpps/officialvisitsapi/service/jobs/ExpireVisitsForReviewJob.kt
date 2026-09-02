package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType.EXPIRE_VISITS_FOR_REVIEW
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

/**
 * This job is responsible for expiring the visits that need to be reviewed.
 *
 * Visits for review will be expired.
 */
@Component
class ExpireVisitsForReviewJob(
  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewService: VisitReviewService,
  timeSource: TimeSource,
) : DailyJob(
  jobType = EXPIRE_VISITS_FOR_REVIEW,
  timeSource,
  { date ->
    officialVisitRepository.findOverdueVisitsWithUnacknowledgedReviewDetailsBefore(date)
  },
  { visits ->
    visits.forEach {
      visitReviewService.expire(it.officialVisitId)
    }
  },
)
