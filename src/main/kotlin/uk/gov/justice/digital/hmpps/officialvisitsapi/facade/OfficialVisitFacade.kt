package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCompletionRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCancellationService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCompletionService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitSearchService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService

@Component
class OfficialVisitFacade(
  private val officialVisitCreateService: OfficialVisitCreateService,
  private val officialVisitsRetrievalService: OfficialVisitsRetrievalService,
  private val officialVisitSearchService: OfficialVisitSearchService,
  private val officialVisitCompletionService: OfficialVisitCompletionService,
  private val officialVisitCancellationService: OfficialVisitCancellationService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun createOfficialVisit(
    prisonCode: String,
    request: CreateOfficialVisitRequest,
    user: User,
  ): CreateOfficialVisitResponse = officialVisitCreateService.create(prisonCode, request, user).also { creationResult ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.VISIT_CREATED,
      prisonCode = prisonCode,
      identifier = creationResult.officialVisitId,
      noms = request.prisonerNumber!!,
      user = user,
    )

    creationResult.officialVisitorIds.forEach { visitorId ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISITOR_CREATED,
        prisonCode = prisonCode,
        identifier = creationResult.officialVisitId,
        secondIdentifier = visitorId,
        // TODO: Should have the contactId for the visitor set here, for use in the PersonReference, but accepts nulls for now
        user = user,
      )
    }
  }

  fun getOfficialVisitByPrisonCodeAndId(prisonCode: String, officialVisitId: Long): OfficialVisitDetails = officialVisitsRetrievalService.getOfficialVisitByPrisonCodeAndId(prisonCode, officialVisitId)

  fun searchForOfficialVisitSummaries(prisonCode: String, request: OfficialVisitSummarySearchRequest, page: Int, size: Int) = officialVisitSearchService.searchForOfficialVisitSummaries(prisonCode, request, page, size)

  fun completeOfficialVisit(
    prisonCode: String,
    officialVisitId: Long,
    request: OfficialVisitCompletionRequest,
    user: User,
  ) {
    officialVisitCompletionService.completeOfficialVisit(prisonCode, officialVisitId, request, user).also { completedVisitDto ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISIT_UPDATED,
        prisonCode = completedVisitDto.prisonCode,
        identifier = completedVisitDto.officialVisitId,
        user = user,
      )

      completedVisitDto.officialVisitorIds.forEach { visitorId ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.VISITOR_UPDATED,
          prisonCode = completedVisitDto.prisonCode,
          identifier = completedVisitDto.officialVisitId,
          secondIdentifier = visitorId,
          // TODO: Should have the contactId for the visitor here, for the PersonReference, but accepts nulls for now
          user = user,
        )
      }

      // TODO: Confirm with Andy whether he needs this event for updated prisoner attendance

      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_UPDATED,
        prisonCode = completedVisitDto.prisonCode,
        identifier = completedVisitDto.officialVisitId,
        secondIdentifier = completedVisitDto.prisonerVisitedId,
        // TODO: Should have the noms = prisonerNumber here, for the PersonReference, but accepts nulls for now
        user = user,
      )
    }
  }

  fun cancelOfficialVisit(
    prisonCode: String,
    officialVisitId: Long,
    request: OfficialVisitCancellationRequest,
    user: User,
  ) {
    officialVisitCancellationService.cancelOfficialVisit(prisonCode, officialVisitId, request, user).also { cancelledVisitDto ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISIT_UPDATED,
        prisonCode = cancelledVisitDto.prisonCode,
        identifier = cancelledVisitDto.officialVisitId,
        user = user,
      )

      cancelledVisitDto.officialVisitorIds.forEach { visitorId ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.VISITOR_UPDATED,
          prisonCode = cancelledVisitDto.prisonCode,
          identifier = cancelledVisitDto.officialVisitId,
          secondIdentifier = visitorId,
          // TODO: Should have the contactId for the visitor here, for the PersonReference, but accepts nulls for now
          user = user,
        )
      }

      // TODO: Confirm with Andy whether he needs this event for updated prisoner attendance

      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_UPDATED,
        prisonCode = cancelledVisitDto.prisonCode,
        identifier = cancelledVisitDto.officialVisitId,
        secondIdentifier = cancelledVisitDto.prisonerVisitedId,
        // TODO: Should have the noms = prisonerNumber here, for the PersonReference, but accepts nulls for now
        user = user,
      )
    }

    // TODO: raise domain events
  }
}
