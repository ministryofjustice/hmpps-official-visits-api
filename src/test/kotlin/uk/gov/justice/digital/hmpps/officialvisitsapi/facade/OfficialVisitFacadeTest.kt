package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService

class OfficialVisitFacadeTest {
  private val request: CreateOfficialVisitRequest = mock()
  private val service: OfficialVisitCreateService = mock()
  private val facade = OfficialVisitFacade(service)

  @Test
  fun `should delegate to service on create`() {
    facade.createOfficialVisit(request, PRISON_USER)

    verify(service).create(request, PRISON_USER)
  }
}
