package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

private val log = LoggerFactory.getLogger(ProcessCandidateVisitsToCheckJob::class.java)

/**
 * This job is responsible for processing the visits that need to be reviewed.
 *
 * Visits will be processed and flagged for review.
 * Processing is done per-prison, but each queued visit is handled in its own transaction so
 * a failure in one visit does not roll back the successful ones from the same prison batch.
 */
@Component
class ProcessCandidateVisitsToCheckJob(
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  private val visitReviewService: VisitReviewService,
  features: FeatureSwitches,
  transactionalPrisonJobProcessor: TransactionalPrisonJobProcessor,
) : PrisonAwareDailyJob<VisitReviewQueueEntity>(
  jobType = JobType.PROCESS_CANDIDATE_VISITS_TO_CHECK,
  features,
  transactionalPrisonJobProcessor,
  { prisonCode ->
    visitReviewQueueRepository.findCandidatesOrderedByQueueTimeForPrison(prisonCode)
  },
  { queueEntries, prisonCode ->
    queueEntries.forEach { queueEntry ->
      try {
        visitReviewService.visitCheckInNewTransaction(queueEntry.officialVisitId, queueEntry.triggeringEvent)
      } catch (exception: Exception) {
        log.error(
          "Failed to process visit review queue item for officialVisitId={} and prisonCode={} and event={}",
          queueEntry.officialVisitId,
          prisonCode,
          queueEntry.triggeringEvent,
          exception,
        )
      }
    }
  },
)
