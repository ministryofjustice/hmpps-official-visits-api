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
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncTimeSlotService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SyncFacadeTest {
  private val syncTimeSlotService: SyncTimeSlotService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val userService: UserService = mock()

  private val facade = SyncFacade(syncTimeSlotService, outboundEventsService, userService)

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
      reset(userService, syncTimeSlotService)
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
  }
}
