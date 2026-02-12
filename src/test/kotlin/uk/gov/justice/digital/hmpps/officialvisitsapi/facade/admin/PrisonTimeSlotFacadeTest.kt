package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin.PrisonTimeSlotService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonTimeSlotFacadeTest {
  private val timeSlotService: PrisonTimeSlotService = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val facade = PrisonTimeSlotFacade(timeSlotService, outboundEventsService)

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @Test
  fun `should send a domain event when a time slot is created`() {
    val request = createTimeSlotRequest()
    val response = timeSlotResponse(prisonTimeSlotId = 1L)

    whenever(timeSlotService.create(any(), any())).thenReturn(response)

    val result = facade.createPrisonTimeSlot(request, MOORLAND_PRISON_USER)

    verify(timeSlotService).create(request, MOORLAND_PRISON_USER)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.TIME_SLOT_CREATED,
      prisonCode = result.prisonCode,
      identifier = result.prisonTimeSlotId,
      source = Source.NOMIS,
      user = MOORLAND_PRISON_USER,
    )
  }

  @Test
  fun `should not send a domain event if failed to create the time slot`() {
    val request = createTimeSlotRequest()
    val expectedException = RuntimeException("Bang!")

    whenever(timeSlotService.create(any(), any())).thenThrow(expectedException)

    val exception = assertThrows<RuntimeException> {
      facade.createPrisonTimeSlot(request, MOORLAND_PRISON_USER)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)

    verify(timeSlotService).create(request, MOORLAND_PRISON_USER)
    verify(outboundEventsService, never()).send(
      outboundEvent = any(),
      prisonCode = any(),
      identifier = any(),
      secondIdentifier = anyOrNull(),
      noms = anyOrNull(),
      contactId = anyOrNull(),
      source = any(),
      user = any(),
    )
  }

  @Test
  fun `should send a domain event when a time slot is updated`() {
    val request = updateTimeSlotRequest()
    val response = timeSlotResponse(prisonTimeSlotId = 2L)

    whenever(timeSlotService.update(prisonTimeSlotId = any(), request = any(), any())).thenReturn(response)

    val result = facade.updatePrisonTimeSlot(2L, request, MOORLAND_PRISON_USER)

    verify(timeSlotService).update(2L, request, MOORLAND_PRISON_USER)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.TIME_SLOT_UPDATED,
      prisonCode = result.prisonCode,
      identifier = result.prisonTimeSlotId,
      source = Source.NOMIS,
      user = MOORLAND_PRISON_USER,
    )
  }

  @Test
  fun `should fail to delete time slot when there is associated visit slot exists and throw EntityInUseException exception`() {
    val expectedException =
      EntityInUseException("The prison time slot has one or more visit slots associated with it and cannot be deleted.")

    whenever(timeSlotService.delete(prisonTimeSlotId = 1L)).thenThrow(expectedException)

    val exception = assertThrows<EntityInUseException> {
      facade.deletePrisonTimeSlot(1L, MOORLAND_PRISON_USER)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(timeSlotService).delete(1)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `should delete time slots if there are no associated visit slots`() {
    val response = timeSlotResponse(prisonTimeSlotId = 1L)

    whenever(timeSlotService.delete(prisonTimeSlotId = 1L)).thenReturn(response)

    facade.deletePrisonTimeSlot(1L, MOORLAND_PRISON_USER)

    verify(timeSlotService).delete(1)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.TIME_SLOT_DELETED,
      prisonCode = MOORLAND,
      identifier = response.prisonTimeSlotId,
      source = Source.NOMIS,
      user = MOORLAND_PRISON_USER,
    )
  }

  private fun createTimeSlotRequest() = CreateTimeSlotRequest(
    prisonCode = MOORLAND,
    dayCode = DayType.MON,
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    effectiveDate = tomorrow(),
    expiryDate = today().plusYears(1),
  )

  private fun updateTimeSlotRequest() = UpdateTimeSlotRequest(
    prisonCode = MOORLAND,
    dayCode = DayType.MON,
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    effectiveDate = tomorrow(),
    expiryDate = today().plusYears(1),
  )

  private fun timeSlotResponse(prisonTimeSlotId: Long) = TimeSlot(
    prisonTimeSlotId = prisonTimeSlotId,
    prisonCode = MOORLAND,
    dayCode = DayType.MON,
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    effectiveDate = tomorrow(),
    expiryDate = today().plusYears(1),
    createdBy = MOORLAND_PRISON_USER.username,
    createdTime = createdTime,
    updatedBy = MOORLAND_PRISON_USER.username,
    updatedTime = updatedTime,
  )
}
