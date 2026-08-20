package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitsForReviewCountResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitForReviewRepository
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class VisitForReviewService(
  private val visitForReviewRepository: VisitForReviewRepository,
) {
  fun countVisitsForReview(prisonCode: String): VisitsForReviewCountResponse = VisitsForReviewCountResponse(
    prisonCode = prisonCode,
    visitsForReviewCount = visitForReviewRepository.countVisitsForReview(
      prisonCode = prisonCode,
      fromDate = LocalDate.now(),
    ),
  )
}
