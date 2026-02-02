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
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncVisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncOfficialVisitService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncTimeSlotService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncVisitSlotService
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class SyncFacadeTest {
  private val syncTimeSlotService: SyncTimeSlotService = mock()
  private val syncVisitSlotService: SyncVisitSlotService = mock()
  private val syncOfficialVisitService: SyncOfficialVisitService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val userService: UserService = mock()

  private val facade = SyncFacade(
    syncTimeSlotService,
    syncVisitSlotService,
    syncOfficialVisitService,
    outboundEventsService,
    userService,
  )

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @BeforeEach
  fun beforeEach() {
    whenever(userService.getUser(MOORLAND_PRISON_USER.username)).thenReturn(MOORLAND_PRISON_USER)
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

  @Nested
  inner class TimeSlotSyncEvents {

    @Test
    fun `should send a domain event when a time slot is created`() {
      val request = createTimeSlotRequest()
      val response = syncTimeSlotResponse(prisonTimeSlotId = 1L)

      whenever(syncTimeSlotService.createPrisonTimeSlot(any())).thenReturn(response)

      val result = facade.createTimeSlot(request)

      verify(syncTimeSlotService).createPrisonTimeSlot(request)

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
      val response = syncTimeSlotResponse(prisonTimeSlotId = 2L)

      whenever(syncTimeSlotService.updatePrisonTimeSlot(prisonTimeSlotId = any(), request = any())).thenReturn(response)

      val result = facade.updateTimeSlot(2L, request)

      verify(syncTimeSlotService).updatePrisonTimeSlot(2L, request)

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

      whenever(syncTimeSlotService.deletePrisonTimeSlot(prisonTimeSlotId = 1L)).thenThrow(expectedException)

      val exception = assertThrows<EntityInUseException> {
        facade.deleteTimeSlot(1L)
      }
      assertThat(exception.message).isEqualTo(expectedException.message)
      verify(syncTimeSlotService).deletePrisonTimeSlot(1)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `should delete time slots if there are no associated visit slots`() {
      val response = syncTimeSlotResponse(prisonTimeSlotId = 1L)
      whenever(syncTimeSlotService.deletePrisonTimeSlot(prisonTimeSlotId = 1L)).thenReturn(response)
      facade.deleteTimeSlot(1L)
      verify(syncTimeSlotService).deletePrisonTimeSlot(1)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.TIME_SLOT_DELETED,
        prisonCode = MOORLAND,
        identifier = response.prisonTimeSlotId,
        source = Source.NOMIS,
        user = MOORLAND_PRISON_USER,
      )
    }

    private fun createTimeSlotRequest() = SyncCreateTimeSlotRequest(
      prisonCode = MOORLAND,
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = tomorrow(),
      expiryDate = today().plusYears(1),
      createdBy = MOORLAND_PRISON_USER.username,
      createdTime = createdTime,
    )

    private fun updateTimeSlotRequest() = SyncUpdateTimeSlotRequest(
      prisonCode = MOORLAND,
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = tomorrow(),
      expiryDate = today().plusYears(1),
      updatedBy = MOORLAND_PRISON_USER.username,
      updatedTime = updatedTime,
    )

    private fun syncTimeSlotResponse(prisonTimeSlotId: Long) = SyncTimeSlot(
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

  @Nested
  inner class VisitSlotSyncEvents {

    @Test
    fun `should send a domain event when a visit slot is created`() {
      val request = createVisitSlotRequest()
      val response = syncVisitSlotResponse(prisonVisitSlotId = 1L)

      whenever(syncVisitSlotService.createPrisonVisitSlot(request)).thenReturn(response)

      val result = facade.createVisitSlot(request)

      verify(syncVisitSlotService).createPrisonVisitSlot(request)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISIT_SLOT_CREATED,
        prisonCode = MOORLAND,
        identifier = result.visitSlotId,
        source = Source.NOMIS,
        user = MOORLAND_PRISON_USER,
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
      val response = syncVisitSlotResponse(prisonVisitSlotId = 1L)

      whenever(
        syncVisitSlotService.updatePrisonVisitSlot(
          prisonVisitSlotId = 1L,
          request = request,
        ),
      ).thenReturn(response)

      val result = facade.updateVisitSlot(1L, request)

      verify(syncVisitSlotService).updatePrisonVisitSlot(1L, request)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISIT_SLOT_UPDATED,
        prisonCode = MOORLAND,
        identifier = result.visitSlotId,
        source = Source.NOMIS,
        user = MOORLAND_PRISON_USER,
      )
    }

    @Test
    fun `should not delete visit slot if associated visits exists`() {
      val expectedException =
        EntityInUseException("The prison visit slot has visits associated with it and cannot be deleted.")

      whenever(syncVisitSlotService.deletePrisonVisitSlot(prisonVisitSlotId = 1L)).thenThrow(expectedException)

      val exception = assertThrows<EntityInUseException> {
        facade.deleteVisitSlot(1L)
      }
      assertThat(exception.message).isEqualTo(expectedException.message)
      verify(syncVisitSlotService).deletePrisonVisitSlot(1)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `should delete a visit slot`() {
      val response = syncVisitSlotResponse(prisonVisitSlotId = 1L)
      whenever(syncVisitSlotService.deletePrisonVisitSlot(prisonVisitSlotId = 1L)).thenReturn(response)
      facade.deleteVisitSlot(1L)
      verify(syncVisitSlotService).deletePrisonVisitSlot(1)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISIT_SLOT_DELETED,
        prisonCode = MOORLAND,
        identifier = response.visitSlotId,
        source = Source.NOMIS,
        user = MOORLAND_PRISON_USER,
      )
    }

    private fun createVisitSlotRequest() = SyncCreateVisitSlotRequest(
      prisonTimeSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      maxAdults = 10,
      createdBy = MOORLAND_PRISON_USER.username,
      createdTime = createdTime,
    )

    private fun updateVisitSlotRequest() = SyncUpdateVisitSlotRequest(
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      updatedBy = MOORLAND_PRISON_USER.username,
      maxAdults = 15,
      updatedTime = updatedTime,
    )

    private fun syncVisitSlotResponse(prisonVisitSlotId: Long) = SyncVisitSlot(
      visitSlotId = prisonVisitSlotId,
      prisonTimeSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      maxAdults = 10,
      createdBy = MOORLAND_PRISON_USER.username,
      createdTime = createdTime,
      updatedBy = MOORLAND_PRISON_USER.username,
      updatedTime = updatedTime,
      prisonCode = MOORLAND,
    )
  }

  @Nested
  inner class OfficialVisitsSyncEvents {

    @Test
    fun `should delegate to service to fetch official visits based on Id`() {
      val response = syncOfficialVisitResponse(1L)
      whenever(syncOfficialVisitService.getOfficialVisitById(officialVisitId = 1L)).thenReturn(response)

      facade.getOfficialVisitById(1L)

      verify(syncOfficialVisitService).getOfficialVisitById(1L)
    }

    private fun syncOfficialVisitResponse(officialVisitId: Long) = SyncOfficialVisit(
      officialVisitId = officialVisitId,
      visitDate = tomorrow(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      prisonCode = MOORLAND,
      prisonerNumber = "A1234AA",
      statusCode = VisitStatusType.SCHEDULED,
      visitType = VisitType.IN_PERSON,
      createdBy = MOORLAND_PRISON_USER.username,
      createdTime = createdTime,
      updatedBy = MOORLAND_PRISON_USER.username,
      updatedTime = updatedTime,
      visitors = listOf(
        SyncOfficialVisitor(
          officialVisitorId = 1L,
          contactId = 2L,
          firstName = "Test",
          lastName = "Testing",
          relationshipType = RelationshipType.OFFICIAL,
          relationshipCode = "POM",
          leadVisitor = false,
          assistedVisit = false,
          createdBy = MOORLAND_PRISON_USER.username,
          createdTime = createdTime,
        ),
      ),
    )
  }
}
