package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository

@Service
class VisitReviewExpireService(
  private val visitReviewRepository: VisitReviewRepository,
) {

  @Transactional
  fun expire(officialVisitId: Long) {
    visitReviewRepository.findByOfficialVisitId(officialVisitId).forEach(VisitReviewEntity::expire)
  }
}
