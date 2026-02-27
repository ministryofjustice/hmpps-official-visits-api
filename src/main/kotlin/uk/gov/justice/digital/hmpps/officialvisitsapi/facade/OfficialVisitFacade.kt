package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCompletionRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateCommentRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCancellationService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCompletionService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitSearchService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitUpdateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
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
  private val officialVisitUpdateService: OfficialVisitUpdateService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun createOfficialVisit(
    prisonCode: String,
    request: CreateOfficialVisitRequest,
    user: User,
  ): CreateOfficialVisitResponse = run {
    require(user is PrisonUser) { "Visits can only be created by a digital prison user" }

    checkPrisonUsersActiveCaseload(
      prisonCode,
      user,
      "This visit cannot be created in a prison which is not the active caseload for the user",
    )

    officialVisitCreateService.create(prisonCode, request, user).also { creationResult ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISIT_CREATED,
        prisonCode = prisonCode,
        identifier = creationResult.officialVisitId,
        noms = creationResult.prisonerNumber,
        user = user,
      )

      creationResult.visitorAndContactIds.forEach { pair ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.VISITOR_CREATED,
          prisonCode = prisonCode,
          identifier = creationResult.officialVisitId,
          secondIdentifier = pair.first,
          contactId = pair.second,
          user = user,
        )
      }
    }
  }

  fun getOfficialVisitByPrisonCodeAndId(prisonCode: String, officialVisitId: Long): OfficialVisitDetails = officialVisitsRetrievalService.getOfficialVisitByPrisonCodeAndId(prisonCode, officialVisitId)

  fun searchForOfficialVisitSummaries(
    prisonCode: String,
    request: OfficialVisitSummarySearchRequest,
    page: Int,
    size: Int,
  ) = officialVisitSearchService.searchForOfficialVisitSummaries(prisonCode, request, page, size)

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
        noms = completedVisitDto.prisonerNumber,
        user = user,
      )

      completedVisitDto.visitorAndContactIds.forEach { pair ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.VISITOR_UPDATED,
          prisonCode = completedVisitDto.prisonCode,
          identifier = completedVisitDto.officialVisitId,
          secondIdentifier = pair.first,
          contactId = pair.second,
          user = user,
        )
      }

      // TODO: Confirm with Andy whether he needs this event for updated prisoner attendance
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_UPDATED,
        prisonCode = completedVisitDto.prisonCode,
        identifier = completedVisitDto.officialVisitId,
        secondIdentifier = completedVisitDto.prisonerVisitedId,
        noms = completedVisitDto.prisonerNumber,
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
        noms = cancelledVisitDto.prisonerNumber,
        user = user,
      )

      cancelledVisitDto.visitorAndContactIds.forEach { pair ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.VISITOR_UPDATED,
          prisonCode = cancelledVisitDto.prisonCode,
          identifier = cancelledVisitDto.officialVisitId,
          secondIdentifier = pair.first,
          contactId = pair.second,
          user = user,
        )
      }

      // TODO: Confirm with Andy whether he needs this event for updated prisoner attendance
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_UPDATED,
        prisonCode = cancelledVisitDto.prisonCode,
        identifier = cancelledVisitDto.officialVisitId,
        secondIdentifier = cancelledVisitDto.prisonerVisitedId,
        noms = cancelledVisitDto.prisonerNumber,
        user = user,
      )
    }
  }

  fun updateVisitTypeAndSlot(officialVisitId: Long, prisonCode: String, request: OfficialVisitUpdateSlotRequest, user: User) {
    val response = officialVisitUpdateService.updateVisitTypeAndSlot(officialVisitId, prisonCode, request, user)
    outboundEventsService.send(
      outboundEvent = OutboundEvent.VISIT_UPDATED,
      prisonCode = prisonCode,
      identifier = response.officialVisitId,
      noms = response.prisonerNumber,
      user = user,
    )
  }

  fun updateComments(officialVisitId: Long, prisonCode: String, request: OfficialVisitUpdateCommentRequest, user: User) {
    val response = officialVisitUpdateService.updateComments(officialVisitId, prisonCode, request, user)
    outboundEventsService.send(
      outboundEvent = OutboundEvent.VISIT_UPDATED,
      prisonCode = prisonCode,
      identifier = response.officialVisitId,
      noms = response.prisonerNumber,
      user = user,
    )
  }

  fun updateVisitors(officialVisitId: Long, prisonCode: String, request: OfficialVisitUpdateVisitorsRequest, user: User) {
    val ov = officialVisitUpdateService.updateVisitors(officialVisitId, prisonCode, request, user)

    outboundEventsService.send(
      outboundEvent = OutboundEvent.VISIT_UPDATED,
      prisonCode = prisonCode,
      identifier = ov.officialVisitId,
      noms = ov.prisonerNumber,
      user = user,
    )

    ov.visitorsUpdated.forEach { visitor ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISITOR_UPDATED,
        prisonCode = ov.prisonCode,
        identifier = ov.officialVisitId,
        secondIdentifier = visitor.officialVisitorId,
        contactId = visitor.contactId,
        user = user,
      )
    }

    ov.visitorsAdded.forEach { visitor ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISITOR_CREATED,
        prisonCode = ov.prisonCode,
        identifier = ov.officialVisitId,
        secondIdentifier = visitor.officialVisitorId,
        contactId = visitor.contactId,
        user = user,
      )
    }

    ov.visitorsDeleted.forEach { visitor ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISITOR_DELETED,
        prisonCode = ov.prisonCode,
        identifier = ov.officialVisitId,
        secondIdentifier = visitor.officialVisitorId,
        contactId = visitor.contactId,
        user = user,
      )
    }
  }

  private fun checkPrisonUsersActiveCaseload(prisonCode: String, user: PrisonUser, message: String) {
    if (prisonCode.trim().uppercase() != user.activeCaseLoadId?.trim()?.uppercase()) {
      throw CaseloadAccessException(message)
    }
  }
}

class CaseloadAccessException(message: String) : RuntimeException(message)
