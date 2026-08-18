package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository

@Component
class VisitReviewChecker(private val visitReviewRepository: VisitReviewRepository) : AbstractChecker(visitReviewRepository) {
  fun check(officialVisit: OfficialVisitEntity) {
    // TODO - to be implemented
  }
}
