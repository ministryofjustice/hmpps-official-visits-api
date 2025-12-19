package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService

@Component
class OfficialVisitFacade(
  private val officialVisitCreateService: OfficialVisitCreateService,
  private val officialVisitService: OfficialVisitsRetrievalService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun getOfficialVisitByPrisonCodeAndId(prisonCode: String, officialVisitId: Long): OfficialVisitDetails = officialVisitService.getOfficialVisitByPrisonCodeAndId(prisonCode, officialVisitId)

  fun createOfficialVisit(
    prisonCode: String,
    request: CreateOfficialVisitRequest,
    user: User,
  ): CreateOfficialVisitResponse = officialVisitCreateService.create(prisonCode, request, user).also { creationResult ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.VISIT_CREATED,
      identifier = creationResult.officialVisitId,
      noms = request.prisonerNumber!!,
      user = user,
    )
  }
}
