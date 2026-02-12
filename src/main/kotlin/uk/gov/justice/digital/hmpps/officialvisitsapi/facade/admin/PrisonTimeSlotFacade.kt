package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin.PrisonTimeSlotService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source

@Component
class PrisonTimeSlotFacade(
  private val prisonTimeSlotService: PrisonTimeSlotService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun getPrisonTimeSlotById(prisonTimeSlotId: Long) = prisonTimeSlotService.getPrisonTimeSlotById(prisonTimeSlotId)
  fun createPrisonTimeSlot(request: CreateTimeSlotRequest, user: User) = prisonTimeSlotService.create(request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.TIME_SLOT_CREATED,
      prisonCode = it.prisonCode,
      identifier = it.prisonTimeSlotId,
      source = Source.DPS,
      user = user,
    )
  }

  fun deletePrisonTimeSlot(prisonTimeSlotId: Long, user: User) = prisonTimeSlotService.delete(prisonTimeSlotId).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.TIME_SLOT_DELETED,
      prisonCode = it.prisonCode,
      identifier = it.prisonTimeSlotId,
      source = Source.DPS,
      user = user,
    )
  }

  fun updatePrisonTimeSlot(prisonTimeSlotId: Long, request: UpdateTimeSlotRequest, user: User) = prisonTimeSlotService.update(prisonTimeSlotId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.TIME_SLOT_UPDATED,
      prisonCode = it.prisonCode,
      identifier = it.prisonTimeSlotId,
      source = Source.DPS,
      user = user,
    )
  }

  fun getAllPrisonTimeSlotsAndAssociatedVisitSlots(prisonCode: String, activeOnly: Boolean) = prisonTimeSlotService.getAllPrisonTimeSlotsAndAssociatedVisitSlots(prisonCode, activeOnly)
}
