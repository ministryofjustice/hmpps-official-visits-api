package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitSummarySearchResponse

@Service
@Transactional(readOnly = true)
class OfficialVisitSearchService {
  fun searchForOfficialVisitSummaries(prisonCode: String, request: OfficialVisitSummarySearchRequest, page: Int, size: Int) = run {
    require(request.endDate!! >= request.startDate) { "End date must be on or after the start date" }
    require(page >= 0) { "Page number must be greater than or equal to zero" }
    require(size > 0) { "Page size must be greater than zero" }

    val pageable = Pageable.ofSize(size).withPage(page)

    // TODO call repo
    val searchResults: List<OfficialVisitSummarySearchResponse> = emptyList()

    PagedModel(PageImpl(searchResults))
  }
}
