package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

/**
 * This job is responsible for processing the visits that need to be reviewed.
 *
 * Visits will be processed and flagged for review.
 * Processing is done per-prison, with each prison's visits handled in a separate transaction.
 */
@Component
class ProcessCandidateVisitsToCheckJob(

  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  private val visitReviewService: VisitReviewService,
  features: FeatureSwitches,
  prisonJobProcessor: PrisonJobProcessor,
  timeSource: TimeSource,
) : PrisonAwareDailyJob<VisitReviewQueueEntity>(
  jobType = JobType.PROCESS_CANDIDATE_VISITS_TO_CHECK,
  timeSource,
  features,
  prisonJobProcessor,
  { date, prisonCode ->
    visitReviewQueueRepository.findCandidatesOrderedByQueueTimeForPrison(prisonCode)
  },
  { queueEntries, prisonCode ->
    queueEntries.forEach { queueEntry ->
      visitReviewService.visitCheck(queueEntry.officialVisitId, queueEntry.triggeringEvent)
    }
  },

)
