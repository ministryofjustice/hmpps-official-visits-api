package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin.VisitSlotService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source

@Component
class VisitSlotFacade(
  private val visitSlotService: VisitSlotService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun createVisitSlot(prisonTimeSlotId: Long, request: CreateVisitSlotRequest, user: User) = visitSlotService.create(prisonTimeSlotId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.VISIT_SLOT_CREATED,
      prisonCode = it.prisonCode,
      identifier = it.visitSlotId,
      source = Source.DPS,
      user = user,
    )
  }

  fun updateVisitSlot(visitSlotId: Long, request: UpdateVisitSlotRequest, user: User) = visitSlotService.update(visitSlotId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.VISIT_SLOT_UPDATED,
      prisonCode = it.prisonCode,
      identifier = it.visitSlotId,
      source = Source.DPS,
      user = user,
    )
  }

  fun deleteVisitSlot(visitSlotId: Long, user: User) {
    visitSlotService.delete(visitSlotId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.VISIT_SLOT_DELETED,
        prisonCode = it.prisonCode,
        identifier = it.visitSlotId,
        source = Source.DPS,
        user = user,
      )
    }
  }
}
