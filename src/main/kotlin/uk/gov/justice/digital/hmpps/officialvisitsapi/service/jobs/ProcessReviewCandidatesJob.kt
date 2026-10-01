package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

private val log = LoggerFactory.getLogger(ProcessReviewCandidatesJob::class.java)

/**
 * This job is responsible for checking whether candidates visits have any issues to be reviewed.
 */
@Component
class ProcessReviewCandidatesJob(
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  private val visitReviewService: VisitReviewService,
  private val featureSwitches: FeatureSwitches,
  private val transactionalPrisonJobProcessor: TransactionalPrisonJobProcessor,
) : VisitReviewJob<VisitReviewQueueEntity>(
  jobType = JobType.PROCESS_REVIEW_CANDIDATES,
  featureSwitches = featureSwitches,
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
