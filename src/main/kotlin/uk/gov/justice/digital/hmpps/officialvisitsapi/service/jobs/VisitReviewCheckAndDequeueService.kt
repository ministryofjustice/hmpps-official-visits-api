package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

@Component
class VisitReviewCheckAndDequeueService(
  private val visitReviewService: VisitReviewService,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
) {

  @Transactional
  fun checkAndDequeue(officialVisitId: Long) {
    visitReviewService.check(officialVisitId, VisitReviewCheckType.CHECK)

    visitReviewQueueRepository.findById(officialVisitId).ifPresent { queueEntry ->
      visitReviewQueueRepository.delete(queueEntry)
    }
  }
}
