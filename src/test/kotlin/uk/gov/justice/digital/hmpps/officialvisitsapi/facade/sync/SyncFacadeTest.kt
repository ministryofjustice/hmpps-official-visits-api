package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncVisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncTimeSlotService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncVisitSlotService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class SyncFacadeTest {
  private val syncTimeSlotService: SyncTimeSlotService = mock()
  private val syncVisitSlotService: SyncVisitSlotService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val userService: UserService = mock()

  private val facade = SyncFacade(syncTimeSlotService, syncVisitSlotService, outboundEventsService, userService)

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @Nested
  inner class TimeSlotSyncEvents {

    @BeforeEach
    fun beforeEach() {
      whenever(userService.getUser("Test")).thenReturn(PrisonUser("MDI", "Test", "Test User"))
      whenever(
        outboundEventsService.send(
          outboundEvent = any(),
          prisonCode = any(),
          identifier = any(),
          secondIdentifier = anyOrNull(),
          noms = anyOrNull(),
          contactId = anyOrNull(),
          source = any(),
          user = any(),
        ),
      ).then {}
    }

    @AfterEach
    fun afterEach() {
      reset(userService, syncTimeSlotService, syncVisitSlotService)
    }

    @Test
    fun `should send a domain event when a time slot is created`() {
      val request = createTimeSlotRequest()
      val response = syncResponse(prisonTimeSlotId = 1L)

      whenever(syncTimeSlotService.createPrisonTimeSlot(any())).thenReturn(response)

      val result = facade.createTimeSlot(request)

      verify(syncTimeSlotService).createPrisonTimeSlot(request)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.TIME_SLOT_CREATED,
        prisonCode = result.prisonCode,
        identifier = result.prisonTimeSlotId,
        source = Source.NOMIS,
        user = PrisonUser("MDI", "Test", "Test User"),
      )
    }

    @Test
    fun `should not send a domain event if failed to create the time slot`() {
      val request = createTimeSlotRequest()
      val expectedException = RuntimeException("Bang!")

      whenever(syncTimeSlotService.createPrisonTimeSlot(any())).thenThrow(expectedException)

      val exception = assertThrows<RuntimeException> {
        facade.createTimeSlot(request)
      }

      assertThat(exception.message).isEqualTo(expectedException.message)

      verify(syncTimeSlotService).createPrisonTimeSlot(request)
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
      val response = syncResponse(prisonTimeSlotId = 2L)

      whenever(syncTimeSlotService.updatePrisonTimeSlot(prisonTimeSlotId = any(), request = any())).thenReturn(response)

      val result = facade.updateTimeSlot(2L, request)

      verify(syncTimeSlotService).updatePrisonTimeSlot(2L, request)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.TIME_SLOT_UPDATED,
        prisonCode = result.prisonCode,
        identifier = result.prisonTimeSlotId,
        source = Source.NOMIS,
        user = PrisonUser("MDI", "Test", "Test User"),
      )
    }

    @Test
    fun `should send a domain event when a visit slot is created`() {
      val request = createVisitSlotRequest()
      val response = syncVisitResponse(prisonVisitSlotId = 1L)

      whenever(syncVisitSlotService.createPrisonVisitSlot(request)).thenReturn(response)

      val result = facade.createVisitSlot(request)

      verify(syncVisitSlotService).createPrisonVisitSlot(request)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISIT_SLOT_CREATED,
        prisonCode = "MDI",
        identifier = result.visitSlotId,
        source = Source.NOMIS,
        user = PrisonUser("MDI", "Test", "Test User"),
      )
    }

    @Test
    fun `should not send a domain event if failed to create the visit slot`() {
      val request = createVisitSlotRequest()
      val expectedException = RuntimeException("Bang!")

      whenever(syncVisitSlotService.createPrisonVisitSlot(request)).thenThrow(expectedException)

      val exception = assertThrows<RuntimeException> {
        facade.createVisitSlot(request)
      }

      assertThat(exception.message).isEqualTo(expectedException.message)

      verify(syncVisitSlotService).createPrisonVisitSlot(request)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `should send a domain event when a visit slot is updated`() {
      val request = updateVisitSlotRequest()
      val response = syncVisitResponse(prisonVisitSlotId = 1L)

      whenever(syncVisitSlotService.updatePrisonVisitSlot(prisonVisitSlotId = 1L, request = request)).thenReturn(response)

      val result = facade.updateVisitSlot(1L, request)

      verify(syncVisitSlotService).updatePrisonVisitSlot(1L, request)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISIT_SLOT_UPDATED,
        prisonCode = "MDI",
        identifier = result.visitSlotId,
        source = Source.NOMIS,
        user = PrisonUser("MDI", "Test", "Test User"),
      )
    }

    @Test
    fun `should not delete visit slot if associated official visit exits and throw EntityInUseException `() {
      val expectedException = EntityInUseException("The prison visit slot has visits associated with it and cannot be deleted.")

      whenever(syncVisitSlotService.deletePrisonVisitSlot(prisonVisitSlotId = 1L)).thenThrow(expectedException)

      val exception = assertThrows<EntityInUseException> {
        facade.deleteVisitSlot(1L)
      }
      assertThat(exception.message).isEqualTo(expectedException.message)
      verify(syncVisitSlotService).deletePrisonVisitSlot(1)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `should fail to  delete time slot when there is associated visit slot exists and throw EntityInUseException exception`() {
      val expectedException = EntityInUseException("The prison time slot has one or more visit slots associated with it and cannot be deleted.")

      whenever(syncTimeSlotService.deletePrisonTimeSlot(prisonTimeSlotId = 1L)).thenThrow(expectedException)

      val exception = assertThrows<EntityInUseException> {
        facade.deleteTimeSlot(1L)
      }
      assertThat(exception.message).isEqualTo(expectedException.message)
      verify(syncTimeSlotService).deletePrisonTimeSlot(1)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `should  delete visits slots if there are no associated visits`() {
      val response = syncVisitResponse(prisonVisitSlotId = 1L)
      whenever(syncVisitSlotService.deletePrisonVisitSlot(prisonVisitSlotId = 1L)).thenReturn(response)
      facade.deleteVisitSlot(1L)
      verify(syncVisitSlotService).deletePrisonVisitSlot(1)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISIT_SLOT_DELETED,
        prisonCode = "MDI",
        identifier = response.visitSlotId,
        source = Source.NOMIS,
        user = PrisonUser("MDI", "Test", "Test User"),
      )
    }

    @Test
    fun `should delete time slots if there are no associated visit slots`() {
      val response = syncResponse(prisonTimeSlotId = 1L)
      whenever(syncTimeSlotService.deletePrisonTimeSlot(prisonTimeSlotId = 1L)).thenReturn(response)
      facade.deleteTimeSlot(1L)
      verify(syncTimeSlotService).deletePrisonTimeSlot(1)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.TIME_SLOT_DELETED,
        prisonCode = "MDI",
        identifier = response.prisonTimeSlotId,
        source = Source.NOMIS,
        user = PrisonUser("MDI", "Test", "Test User"),
      )
    }

    private fun createTimeSlotRequest() = SyncCreateTimeSlotRequest(
      prisonCode = "MDI",
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = LocalDate.now().plusDays(1),
      expiryDate = LocalDate.now().plusDays(365),
      createdBy = "Test",
      createdTime = createdTime,
    )

    private fun updateTimeSlotRequest() = SyncUpdateTimeSlotRequest(
      prisonCode = "MDI",
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = LocalDate.now().plusDays(1),
      expiryDate = LocalDate.now().plusDays(365),
      updatedBy = "Test",
      updatedTime = updatedTime,
    )

    private fun syncResponse(prisonTimeSlotId: Long) = SyncTimeSlot(
      prisonTimeSlotId = prisonTimeSlotId,
      prisonCode = "MDI",
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = LocalDate.now().plusDays(1),
      expiryDate = LocalDate.now().plusDays(365),
      createdBy = "Test",
      createdTime = createdTime,
      updatedBy = "Test",
      updatedTime = updatedTime,
    )

    private fun createVisitSlotRequest() = SyncCreateVisitSlotRequest(
      prisonTimeSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      maxAdults = 10,
      createdBy = "Test",
      createdTime = createdTime,
    )

    private fun updateVisitSlotRequest() = SyncUpdateVisitSlotRequest(
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      updatedBy = "Test",
      maxAdults = 15,
      updatedTime = updatedTime,
    )

    private fun syncVisitResponse(prisonVisitSlotId: Long) = SyncVisitSlot(
      visitSlotId = prisonVisitSlotId,
      prisonTimeSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      maxAdults = 10,
      createdBy = "Test",
      createdTime = createdTime,
      updatedBy = "Test",
      updatedTime = updatedTime,
      prisonCode = "MDI",
    )
  }
}
