package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitSearchService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService

class OfficialVisitFacadeTest {
  private val service: OfficialVisitCreateService = mock()
  private val officialVisitService: OfficialVisitsRetrievalService = mock()
  private val officialVisitSearchService: OfficialVisitSearchService = mock()
  private val facade = OfficialVisitFacade(service, officialVisitService, officialVisitSearchService)

  @Test
  fun `should delegate to correct service on create`() {
    val request: CreateOfficialVisitRequest = mock()

    facade.createOfficialVisit(PENTONVILLE, request, PENTONVILLE_PRISON_USER)

    verify(service).create(PENTONVILLE, request, PENTONVILLE_PRISON_USER)
  }

  @Test
  fun `should delegate to service to fetch official visits based on Id`() {
    facade.getOfficialVisitByPrisonCodeAndId("MIC", 1L)
    verify(officialVisitService).getOfficialVisitByPrisonCodeAndId("MIC", 1L)
  }

  @Test
  fun `should delegate to correct service on search`() {
    val request: OfficialVisitSummarySearchRequest = mock()

    facade.searchForOfficialVisitSummaries(MOORLAND, request, 0, 10)

    verify(officialVisitSearchService).searchForOfficialVisitSummaries(MOORLAND, request, 0, 10)
  }
}
