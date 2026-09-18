package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType

/**
 * This job is responsible for identifying the visits that need to be reviewed.
 *
 * Visits  will be checked and flagged for review.
 */
@Component
class IdentifyCandidateVisitsToCheckJob(

  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  timeSource: TimeSource,
) : DailyJob<Long>(
  jobType = JobType.IDENTIFY_CANDIDATE_VISITS_TO_CHECK,
  timeSource,
  { date ->
    officialVisitRepository.findCandidateVisitsForReview(date.plusDays(7))
  },
  { officialVisitId ->
    officialVisitId.forEach {
      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = it,
          createdTime = timeSource.now(),
          triggeringEvent = VisitReviewCheckType.CHECK,
        ),
      )
    }
  },

)
