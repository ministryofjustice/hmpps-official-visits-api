package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

@Service
class VisitsForReviewService(
  private val visitReviewRepository: VisitReviewRepository,
  private val visitReviewService: VisitReviewService,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
) {

  @Transactional
  fun expire(officialVisitId: Long) {
    visitReviewRepository.findByOfficialVisitId(officialVisitId).forEach(VisitReviewEntity::expire)
  }

  @Transactional
  fun check(officialVisitId: Long) {
    visitReviewService.check(officialVisitId, VisitReviewCheckType.CHECK)

    visitReviewQueueRepository.findById(officialVisitId).ifPresent { queueEntry ->
      visitReviewQueueRepository.delete(queueEntry)
    }
  }
}
