package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository

/**
 * This job is responsible for checking the visits that need to be reviewed.
 *
 * Visits  will be checked and flagged for review.
 */
@Component
class ProcessCandidateVisitsToCheckJob(

  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewCheckAndDequeueService: VisitReviewCheckAndDequeueService,
  timeSource: TimeSource,
) : DailyJob(
  jobType = JobType.PROCESS_CANDIDATE_VISITS_TO_CHECK,
  timeSource,
  { date ->
    officialVisitRepository.findCandidatesOrderedByQueueTime()
  },
  { visits ->
    visits.forEach {
      visitReviewCheckAndDequeueService.checkAndDequeue(it.officialVisitId)
    }
  },

)
