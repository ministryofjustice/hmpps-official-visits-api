package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin.VisitSlotService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import java.time.LocalDateTime
import java.util.UUID

class VisitSlotFacadeTest {
  private val visitSlotService: VisitSlotService = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val facade = VisitSlotFacade(visitSlotService, outboundEventsService)

  private val createdTime = LocalDateTime.now().minusDays(2)

  @Test
  fun `should send a domain event when a visit slot is created`() {
    val request = CreateVisitSlotRequest(dpsLocationId = UUID.randomUUID(), maxAdults = 1, maxGroups = 1, maxVideo = 1)
    val response = VisitSlot(
      visitSlotId = 1,
      prisonCode = "MDI",
      prisonTimeSlotId = 1,
      dpsLocationId = request.dpsLocationId,
      createdBy = MOORLAND_PRISON_USER.username,
      createdTime = createdTime,
      maxAdults = 1,
      maxGroups = 1,
      maxVideo = 1,
    )

    whenever(visitSlotService.create(any(), any(), any())).thenReturn(response)

    val result = facade.createVisitSlot(1L, request, MOORLAND_PRISON_USER)

    verify(visitSlotService).create(1L, request, MOORLAND_PRISON_USER)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISIT_SLOT_CREATED,
      prisonCode = result.prisonCode,
      identifier = result.visitSlotId,
      source = Source.DPS,
      user = MOORLAND_PRISON_USER,
    )
  }

  @Test
  fun `should not send a domain event if it fails to create the visit slot`() {
    val request = CreateVisitSlotRequest(dpsLocationId = UUID.randomUUID(), maxAdults = 1, maxGroups = 1, maxVideo = 1)
    val expectedException = RuntimeException("Bang!")

    whenever(visitSlotService.create(any(), any(), any())).thenThrow(expectedException)

    val exception = assertThrows<RuntimeException> {
      facade.createVisitSlot(1L, request, MOORLAND_PRISON_USER)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)

    verify(visitSlotService).create(1L, request, MOORLAND_PRISON_USER)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `should send a domain event when a visit slot is updated`() {
    val request = UpdateVisitSlotRequest(maxAdults = 2, maxGroups = 2, maxVideo = 0)
    val response = VisitSlot(
      visitSlotId = 2,
      prisonCode = "MDI",
      prisonTimeSlotId = 1,
      dpsLocationId = UUID.randomUUID(),
      createdBy = MOORLAND_PRISON_USER.username,
      createdTime = createdTime,
      maxAdults = 2,
      maxGroups = 2,
      maxVideo = 0,
    )

    // use positional any() to match parameters
    whenever(visitSlotService.update(any(), any(), any())).thenReturn(response)

    val result = facade.updateVisitSlot(2L, request, MOORLAND_PRISON_USER)

    verify(visitSlotService).update(2L, request, MOORLAND_PRISON_USER)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISIT_SLOT_UPDATED,
      prisonCode = result.prisonCode,
      identifier = result.visitSlotId,
      source = Source.DPS,
      user = MOORLAND_PRISON_USER,
    )
  }

  @Test
  fun `should not send a domain event if it fails to update the visit slot`() {
    val request = UpdateVisitSlotRequest(maxAdults = 2, maxGroups = 2, maxVideo = 0)
    val expectedException = RuntimeException("Bang!")

    whenever(visitSlotService.update(any(), any(), any())).thenThrow(expectedException)

    val exception = assertThrows<RuntimeException> {
      facade.updateVisitSlot(1L, request, MOORLAND_PRISON_USER)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)

    verify(visitSlotService).update(1L, request, MOORLAND_PRISON_USER)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `should fail to delete visit slot when there is associated official visits exists and throw EntityInUseException exception`() {
    val expectedException = EntityInUseException("The prison visit slot has visits associated with it and cannot be deleted.")

    whenever(visitSlotService.delete(any())).thenThrow(expectedException)

    val exception = assertThrows<EntityInUseException> {
      facade.deleteVisitSlot(1L, MOORLAND_PRISON_USER)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(visitSlotService).delete(1)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `should delete visit slot if there are no associated official visits`() {
    val response = VisitSlot(
      visitSlotId = 1,
      prisonCode = "MDI",
      prisonTimeSlotId = 1,
      dpsLocationId = UUID.randomUUID(),
      createdBy = MOORLAND_PRISON_USER.username,
      createdTime = createdTime,
      maxAdults = 1,
    )

    whenever(visitSlotService.delete(any())).thenReturn(response)

    facade.deleteVisitSlot(1L, MOORLAND_PRISON_USER)

    verify(visitSlotService).delete(1)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISIT_SLOT_DELETED,
      prisonCode = response.prisonCode,
      identifier = response.visitSlotId,
      source = Source.DPS,
      user = MOORLAND_PRISON_USER,
    )
  }
}
