package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User

@Component
class OfficialVisitFacade(private val officialVisitCreateService: OfficialVisitCreateService) {
  fun createOfficialVisit(request: CreateOfficialVisitRequest, user: User): CreateOfficialVisitResponse = officialVisitCreateService.create(request, user)
}
