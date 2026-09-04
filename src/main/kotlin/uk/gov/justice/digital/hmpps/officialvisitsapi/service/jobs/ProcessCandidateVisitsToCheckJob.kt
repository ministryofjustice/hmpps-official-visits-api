package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

/**
 * This job is responsible for processing the visits that need to be reviewed.
 *
 * Visits  will be processed and flagged for review.
 */
@Component
class ProcessCandidateVisitsToCheckJob(

  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewService: VisitReviewService,
  timeSource: TimeSource,
) : DailyJob(
  jobType = JobType.PROCESS_CANDIDATE_VISITS_TO_CHECK,
  timeSource,
  { date ->
    officialVisitRepository.findCandidatesOrderedByQueueTime()
  },
  { visits ->
    visits.forEach {
      visitReviewService.visitCheck(it.officialVisitId, VisitReviewCheckType.CHECK)
    }
  },

)
