package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import java.time.LocalDateTime

@Component
class VisitReviewExpireService(
  private val visitReviewRepository: VisitReviewRepository,
) {

  @Transactional
  fun expire(officialVisitId: Long) {
    visitReviewRepository.findByOfficialVisitId(officialVisitId)?.let { visitReview ->
      visitReview.forEach { review ->
        review.expiredTime = LocalDateTime.now()
      }
    }
  }
}
