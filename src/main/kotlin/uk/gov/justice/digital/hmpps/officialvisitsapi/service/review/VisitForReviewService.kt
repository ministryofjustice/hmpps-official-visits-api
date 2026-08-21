package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitForReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitForReviewIssue
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitsForReviewCountResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitsForReviewResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitForReviewRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class VisitForReviewService(
  private val visitForReviewRepository: VisitForReviewRepository,
  private val officialVisitsRetrievalService: OfficialVisitsRetrievalService,
) {

  fun countVisitsForReview(prisonCode: String): VisitsForReviewCountResponse = VisitsForReviewCountResponse(
    prisonCode = prisonCode,
    visitsForReviewCount = visitForReviewRepository.countVisitsForReview(
      prisonCode = prisonCode,
      fromDate = LocalDate.now(),
    ),
  )

  fun getVisitsForReview(prisonCode: String, pageable: Pageable): PagedModel<VisitsForReviewResponse> {
    val fromDate = LocalDate.now()
    val visitIdsPage = visitForReviewRepository.findVisitIdsForReview(
      prisonCode = prisonCode,
      fromDate = fromDate,
      pageable = pageable,
    )

    if (visitIdsPage.isEmpty) {
      return PagedModel(PageImpl(emptyList(), pageable, visitIdsPage.totalElements))
    }

    val detailsByVisitId = visitForReviewRepository.findCurrentReviewDetailsForVisitIds(
      officialVisitIds = visitIdsPage.content,
      fromDate = fromDate,
    ).groupBy { it.officialVisitId }

    val response = visitIdsPage.content.map { officialVisitId ->
      VisitsForReviewResponse(
        visit = officialVisitsRetrievalService.getOfficialVisitByPrisonCodeAndId(prisonCode, officialVisitId),
        issues = detailsByVisitId[officialVisitId].orEmpty()
          .sortedBy { it.detailRaisedTime }
          .map { it.toIssue() },
      )
    }

    return PagedModel(PageImpl(response, pageable, visitIdsPage.totalElements))
  }

  private fun VisitForReviewEntity.toIssue() = VisitForReviewIssue(
    visitReviewDetailId = visitReviewDetailId,
    issueType = issueType,
    issueDetail = issueDetail,
    raisedTime = detailRaisedTime,
  )
}
