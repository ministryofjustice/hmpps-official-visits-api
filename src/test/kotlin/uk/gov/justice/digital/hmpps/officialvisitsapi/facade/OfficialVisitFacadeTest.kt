package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitService

class OfficialVisitFacadeTest {
  private val request: CreateOfficialVisitRequest = mock()
  private val service: OfficialVisitCreateService = mock()
  private val officialVisitService: OfficialVisitService = mock()
  private val facade = OfficialVisitFacade(service, officialVisitService)

  @Test
  fun `should delegate to service on create`() {
    facade.createOfficialVisit(request, PENTONVILLE_PRISON_USER)

    verify(service).create(request, PENTONVILLE_PRISON_USER)
  }

  @Test
  fun `should delegate to service to fetch official visits based on Id`() {
    facade.getOfficialVisitById(1L)
    verify(officialVisitService).getOfficialVisitById(1L)
  }
}
