package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncOfficialVisitService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncOfficialVisitorService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncTimeSlotService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncVisitSlotService

/**
 * This class is a facade over the sync services as a thin layer
 * which is called by the sync controllers and in-turn calls the sync
 * service methods.
 *
 * Each method provides two purposes:
 * - To call the underlying sync services and apply the changes in a transactional method.
 * - To generate a domain event to inform subscribed services what has happened.
 *
 * All events generated as a result of a sync operation should generate domain events with the
 * additionalInformation source = "NOMIS", which indicates that the actual source of the
 * original change was NOMIS.
 *
 * This is important, as the Syscon sync service will ignore domain events with
 * a source of NOMIS, but will action those with a source of DPS for changes which
 * originate within this service via the UI or local processes.
 */

@Service
class SyncFacade(
  val syncTimeSlotService: SyncTimeSlotService,
  val syncVisitSlotService: SyncVisitSlotService,
  val syncOfficialVisitService: SyncOfficialVisitService,
  val syncOfficialVisitorService: SyncOfficialVisitorService,
  val outboundEventsService: OutboundEventsService,
) {

  // ---------------  Time slots ----------------------

  fun getTimeSlotById(prisonTimeSlotId: Long) = syncTimeSlotService.getPrisonTimeSlotById(prisonTimeSlotId)

  fun createTimeSlot(request: SyncCreateTimeSlotRequest) = syncTimeSlotService.createPrisonTimeSlot(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.TIME_SLOT_CREATED,
        prisonCode = it.prisonCode,
        identifier = it.prisonTimeSlotId,
        source = Source.NOMIS,
        username = request.createdBy,
      )
    }

  fun updateTimeSlot(prisonTimeSlotId: Long, request: SyncUpdateTimeSlotRequest) = syncTimeSlotService.updatePrisonTimeSlot(prisonTimeSlotId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.TIME_SLOT_UPDATED,
        prisonCode = it.prisonCode,
        identifier = it.prisonTimeSlotId,
        source = Source.NOMIS,
        username = request.updatedBy,
      )
    }

  fun deleteTimeSlot(timeSlotId: Long) {
    syncTimeSlotService.deletePrisonTimeSlot(timeSlotId)?.also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.TIME_SLOT_DELETED,
        prisonCode = it.prisonCode,
        identifier = it.prisonTimeSlotId,
        source = Source.NOMIS,
        username = "NOMIS",
      )
    }
  }

  // ---------------  Visit slots ----------------------

  fun getVisitSlotById(prisonVisitSlotId: Long) = syncVisitSlotService.getPrisonVisitSlotById(prisonVisitSlotId)

  fun createVisitSlot(request: SyncCreateVisitSlotRequest) = syncVisitSlotService.createPrisonVisitSlot(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISIT_SLOT_CREATED,
        prisonCode = it.prisonCode,
        identifier = it.visitSlotId,
        source = Source.NOMIS,
        username = request.createdBy,
      )
    }

  fun updateVisitSlot(prisonVisitSlotId: Long, request: SyncUpdateVisitSlotRequest) = syncVisitSlotService.updatePrisonVisitSlot(prisonVisitSlotId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISIT_SLOT_UPDATED,
        prisonCode = it.prisonCode,
        identifier = it.visitSlotId,
        source = Source.NOMIS,
        username = request.updatedBy ?: "NOMIS",
      )
    }

  fun deleteVisitSlot(visitSlotId: Long) {
    syncVisitSlotService.deletePrisonVisitSlot(visitSlotId)
      ?.also {
        outboundEventsService.send(
          outboundEvent = OutboundEvent.VISIT_SLOT_DELETED,
          prisonCode = it.prisonCode,
          identifier = it.visitSlotId,
          source = Source.NOMIS,
          username = "NOMIS",
        )
      }
  }

  // ---------------  Official visits ----------------------

  fun getOfficialVisitById(officialVisitId: Long) = syncOfficialVisitService.getVisitById(officialVisitId)

  fun createOfficialVisit(request: SyncCreateOfficialVisitRequest) = syncOfficialVisitService.createVisit(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISIT_CREATED,
        prisonCode = it.prisonCode,
        identifier = it.officialVisitId,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
        username = it.createdBy,
      )
    }

  fun updateOfficialVisit(officialVisitId: Long, request: SyncUpdateOfficialVisitRequest) = syncOfficialVisitService.updateVisit(officialVisitId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISIT_UPDATED,
        prisonCode = it.prisonCode,
        identifier = it.officialVisitId,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
        username = it.updatedBy ?: "NOMIS",
      )
    }

  fun deleteOfficialVisit(officialVisitId: Long) {
    syncOfficialVisitService.deleteVisit(officialVisitId)
      ?.also { deletedOfficialVisit ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.VISIT_DELETED,
          prisonCode = deletedOfficialVisit.prisonCode,
          identifier = deletedOfficialVisit.officialVisitId,
          source = Source.NOMIS,
          noms = deletedOfficialVisit.prisonerNumber,
          username = "NOMIS",
        )

        deletedOfficialVisit.visitors.forEach { visitor ->
          outboundEventsService.send(
            outboundEvent = OutboundEvent.VISITOR_DELETED,
            prisonCode = deletedOfficialVisit.prisonCode,
            identifier = deletedOfficialVisit.officialVisitId,
            secondIdentifier = visitor.officialVisitorId,
            contactId = visitor.contactId,
            source = Source.NOMIS,
            username = "NOMIS",
          )
        }
      }
  }

  // ---------------  Official visitors ----------------------

  fun createOfficialVisitor(officialVisitId: Long, request: SyncCreateOfficialVisitorRequest): SyncOfficialVisitor {
    val response = syncOfficialVisitorService.createVisitor(officialVisitId, request)
    outboundEventsService.send(
      outboundEvent = OutboundEvent.VISITOR_CREATED,
      prisonCode = response.prisonCode,
      identifier = response.officialVisitId,
      secondIdentifier = response.officialVisitorId,
      contactId = response.visitor.contactId,
      source = Source.NOMIS,
      username = response.visitor.createdBy,
    )
    return response.visitor
  }

  fun updateOfficialVisitor(officialVisitId: Long, officialVisitorId: Long, request: SyncUpdateOfficialVisitorRequest): SyncOfficialVisitor {
    val response = syncOfficialVisitorService.updateVisitor(officialVisitId, officialVisitorId, request)
    outboundEventsService.send(
      outboundEvent = OutboundEvent.VISITOR_UPDATED,
      prisonCode = response.prisonCode,
      identifier = response.officialVisitId,
      secondIdentifier = response.officialVisitorId,
      contactId = response.visitor.contactId,
      source = Source.NOMIS,
      username = response.visitor.updatedBy ?: "NOMIS",
    )
    return response.visitor
  }

  fun deleteOfficialVisitor(officialVisitId: Long, officialVisitorId: Long) {
    syncOfficialVisitorService.deleteVisitor(officialVisitId, officialVisitorId)?.also { response ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISITOR_DELETED,
        prisonCode = response.prisonCode,
        identifier = response.officialVisitId,
        secondIdentifier = response.officialVisitorId,
        contactId = response.contactId,
        source = Source.NOMIS,
        username = "NOMIS",
      )
    }
  }
}
