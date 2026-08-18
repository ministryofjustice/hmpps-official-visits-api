package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository

abstract class AbstractChecker(private val visitReviewRepository: VisitReviewRepository) {
  protected fun getExistingVisitReview(officialVisitId: Long): VisitReviewEntity? = run {
    visitReviewRepository.findByOfficialVisitId(officialVisitId).singleOrNull { it.expiredTime == null }
  }
}
