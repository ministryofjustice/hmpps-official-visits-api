package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User

@Component
class OfficialVisitFacade(private val officialVisitCreateService: OfficialVisitCreateService, private val officialVisitService: OfficialVisitService) {
  // TODO check there is still an available slot prior to creation!!
  fun createOfficialVisit(request: CreateOfficialVisitRequest, user: User): CreateOfficialVisitResponse = officialVisitCreateService.create(request, user)
  fun getOfficialVisitByPrisonCodeAndId(prisonCode: String, officialVisitId: Long): OfficialVisitDetails = officialVisitService.getOfficialVisitByPrisonCodeAndId(prisonCode, officialVisitId)
}
