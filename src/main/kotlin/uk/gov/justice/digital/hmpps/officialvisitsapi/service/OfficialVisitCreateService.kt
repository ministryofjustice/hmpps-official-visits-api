package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse

@Service
class OfficialVisitCreateService {
  fun create(request: CreateOfficialVisitRequest, user: User): CreateOfficialVisitResponse = CreateOfficialVisitResponse(-1)
}
