package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

/**
 * This job is responsible for processing the visits that need to be reviewed.
 *
 * Visits  will be processed and flagged for review.
 */
@Component
class ProcessCandidateVisitsToCheckJob(

  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  private val visitReviewService: VisitReviewService,
  timeSource: TimeSource,
) : DailyJob<VisitReviewQueueEntity>(
  jobType = JobType.PROCESS_CANDIDATE_VISITS_TO_CHECK,
  timeSource,
  { date ->
    visitReviewQueueRepository.findCandidatesOrderedByQueueTime()
  },
  { queueEntries ->
    queueEntries.forEach {
      visitReviewService.visitCheck(it.officialVisitId, it.triggeringEvent)
    }
  },

)
