package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User

@Component
class OfficialVisitFacade(private val officialVisitCreateService: OfficialVisitCreateService, private val officialVisitService: OfficialVisitsRetrievalService) {
  fun createOfficialVisit(prisonCode: String, request: CreateOfficialVisitRequest, user: User): CreateOfficialVisitResponse = officialVisitCreateService.create(prisonCode, request, user)
  fun getOfficialVisitByPrisonCodeAndId(prisonCode: String, officialVisitId: Long): OfficialVisitDetails = officialVisitService.getOfficialVisitByPrisonCodeAndId(prisonCode, officialVisitId)
}
