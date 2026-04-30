package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.sync

import jakarta.persistence.EntityNotFoundException
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
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitDeletionInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitorDeletionInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncVisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncAddVisitorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncOfficialVisitService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncOfficialVisitorService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncRemoveVisitorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncTimeSlotService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncUpdateVisitorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.SyncVisitSlotService
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class SyncFacadeTest {
  private val syncTimeSlotService: SyncTimeSlotService = mock()
  private val syncVisitSlotService: SyncVisitSlotService = mock()
  private val syncOfficialVisitService: SyncOfficialVisitService = mock()
  private val syncOfficialVisitorService: SyncOfficialVisitorService = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val facade = SyncFacade(
    syncTimeSlotService,
    syncVisitSlotService,
    syncOfficialVisitService,
    syncOfficialVisitorService,
    outboundEventsService,
  )

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @BeforeEach
  fun beforeEach() {
    whenever(
      outboundEventsService.send(
        outboundEvent = any(),
        prisonCode = any(),
        identifier = any(),
        secondIdentifier = anyOrNull(),
        noms = anyOrNull(),
        contactId = anyOrNull(),
        source = any(),
        username = any(),
      ),
    ).then {}
  }

  @AfterEach
  fun afterEach() {
    reset(
      syncTimeSlotService,
      syncVisitSlotService,
      syncOfficialVisitService,
      syncOfficialVisitorService,
      outboundEventsService,
    )
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
        username = MOORLAND_PRISON_USER.username,
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
        username = any(),
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
        username = MOORLAND_PRISON_USER.username,
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
        username = "NOMIS",
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
        username = MOORLAND_PRISON_USER.username,
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
        username = MOORLAND_PRISON_USER.username,
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
        username = "NOMIS",
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
    fun `create a visit - should delegate the create and raise an event`() {
      val officialVisitId = 1L
      val request = createVisitRequest(visitSlotId = 1L)
      val response = syncOfficialVisitResponse(officialVisitId)

      whenever(syncOfficialVisitService.createVisit(request)).thenReturn(response)

      facade.createOfficialVisit(request)

      verify(syncOfficialVisitService).createVisit(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISIT_CREATED,
        prisonCode = MOORLAND,
        source = Source.NOMIS,
        identifier = officialVisitId,
        noms = MOORLAND_PRISONER.number,
        username = MOORLAND_PRISON_USER.username,
      )
    }

    @Test
    fun `create a visit - should fail the create and avoid raising an event`() {
      val request = createVisitRequest(visitSlotId = 1L)

      whenever(syncOfficialVisitService.createVisit(request)).thenThrow(
        EntityNotFoundException("Prison visit slot ID ${request.prisonVisitSlotId} does not exist"),
      )

      assertThrows<EntityNotFoundException> {
        facade.createOfficialVisit(request)
      }

      verify(syncOfficialVisitService).createVisit(request)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update a visit - should delegate the update and raise an event`() {
      val officialVisitId = 1L
      val request = updateVisitRequest(visitSlotId = 1L)
      val response = syncOfficialVisitResponse(officialVisitId)

      whenever(syncOfficialVisitService.updateVisit(officialVisitId, request)).thenReturn(response)

      facade.updateOfficialVisit(officialVisitId, request)

      verify(syncOfficialVisitService).updateVisit(officialVisitId, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISIT_UPDATED,
        prisonCode = MOORLAND,
        source = Source.NOMIS,
        identifier = officialVisitId,
        noms = MOORLAND_PRISONER.number,
        username = MOORLAND_PRISON_USER.username,
      )
    }

    @Test
    fun `update a visit - should fail the update and avoid raising an event`() {
      val officialVisitId = 1L
      val request = updateVisitRequest(visitSlotId = 1L)

      whenever(syncOfficialVisitService.updateVisit(officialVisitId, request)).thenThrow(
        EntityNotFoundException("Official visit with id $officialVisitId not found"),
      )

      assertThrows<EntityNotFoundException> {
        facade.updateOfficialVisit(officialVisitId, request)
      }

      verify(syncOfficialVisitService).updateVisit(officialVisitId, request)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `get a visit - should delegate to the service and return the visit`() {
      val response = syncOfficialVisitResponse(1L)

      whenever(syncOfficialVisitService.getVisitById(officialVisitId = 1L)).thenReturn(response)

      facade.getOfficialVisitById(1L)

      verify(syncOfficialVisitService).getVisitById(1L)
    }

    @Test
    fun `delete a visit - should return success if the visit ID is not present`() {
      whenever(syncOfficialVisitService.deleteVisit(99L)).thenReturn(null)

      facade.deleteOfficialVisit(99L)

      verify(syncOfficialVisitService).deleteVisit(99L)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `delete a visit - should delete the visit and raise the event`() {
      val response = syncOfficialVisitDeleted(1L)

      whenever(syncOfficialVisitService.deleteVisit(1L)).thenReturn(response)

      facade.deleteOfficialVisit(1L)

      verify(syncOfficialVisitService).deleteVisit(1L)

      verify(outboundEventsService, atLeastOnce()).send(
        outboundEvent = OutboundEvent.VISIT_DELETED,
        prisonCode = MOORLAND,
        source = Source.NOMIS,
        identifier = 1L, // official visit ID
        noms = "A1234AA",
        username = "NOMIS",
      )

      verify(outboundEventsService, atLeastOnce()).send(
        outboundEvent = OutboundEvent.VISITOR_DELETED,
        prisonCode = MOORLAND,
        source = Source.NOMIS,
        identifier = 1L, // official visit ID
        secondIdentifier = 1L, // official visitor ID
        contactId = 2L,
        username = "NOMIS",
      )
    }

    private fun syncOfficialVisitDeleted(officialVisitId: Long) = SyncOfficialVisitDeletionInfo(
      officialVisitId = officialVisitId,
      prisonCode = MOORLAND,
      prisonerNumber = "A1234AA",
      visitors = listOf(
        SyncOfficialVisitorDeletionInfo(
          officialVisitorId = 1L,
          contactId = 2L,
        ),
      ),
    )

    private fun createVisitRequest(visitSlotId: Long) = SyncCreateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = visitSlotId,
      prisonCode = MOORLAND,
      offenderBookId = 1L,
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = tomorrow(),
      startTime = LocalTime.parse("09:00"),
      endTime = LocalTime.parse("10:00"),
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      createDateTime = createdTime,
      createUsername = MOORLAND_PRISON_USER.username,
    )

    private fun updateVisitRequest(visitSlotId: Long) = SyncUpdateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = visitSlotId,
      prisonCode = MOORLAND,
      offenderBookId = 1L,
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = tomorrow().plusDays(2),
      startTime = LocalTime.parse("10:00"),
      endTime = LocalTime.parse("11:00"),
      visitorConcernText = "Concerned",
      visitStatusCode = VisitStatusType.CANCELLED,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      updateDateTime = updatedTime,
      updateUsername = MOORLAND_PRISON_USER.username,
    )

    private fun syncOfficialVisitResponse(officialVisitId: Long) = SyncOfficialVisit(
      officialVisitId = officialVisitId,
      visitDate = tomorrow(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      prisonCode = MOORLAND,
      prisonerNumber = MOORLAND_PRISONER.number,
      statusCode = VisitStatusType.SCHEDULED,
      visitType = VisitType.IN_PERSON,
      createdBy = MOORLAND_PRISON_USER.username,
      createdTime = createdTime,
      updatedBy = MOORLAND_PRISON_USER.username,
      updatedTime = updatedTime,
      currentTerm = true,
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

  @Nested
  inner class OfficialVisitorSyncEvents {
    val officialVisitId = 1L
    val officialVisitorId = 2L
    val contactId = 3L
    val creationTime: LocalDateTime = LocalDateTime.now()
    val updateTime: LocalDateTime = LocalDateTime.now()

    @Test
    fun `add a visitor - should add a visitor to a visit and raise an event`() {
      val request = createVisitorRequest()
      val mockedResponse = createVisitorResponse()
      whenever(syncOfficialVisitorService.createVisitor(officialVisitId, request)).thenReturn(
        mockedResponse,
      )

      val response = facade.createOfficialVisitor(officialVisitId, request)

      assertThat(response.officialVisitorId).isEqualTo(officialVisitorId)

      verify(syncOfficialVisitorService).createVisitor(officialVisitId, request)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISITOR_CREATED,
        prisonCode = MOORLAND,
        source = Source.NOMIS,
        identifier = officialVisitId,
        secondIdentifier = officialVisitorId,
        contactId = contactId,
        username = MOORLAND_PRISON_USER.username,
      )
    }

    @Test
    fun `add a visitor - should fail if the visit was not found`() {
      val request = createVisitorRequest()
      val expectedException = EntityNotFoundException("The official visit with id $officialVisitId was not found")

      whenever(syncOfficialVisitorService.createVisitor(officialVisitId, request)).thenThrow(expectedException)

      val exception = assertThrows<EntityNotFoundException> {
        facade.createOfficialVisitor(officialVisitId, request)
      }

      assertThat(exception.message).isEqualTo(expectedException.message)

      verify(syncOfficialVisitorService).createVisitor(officialVisitId, request)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `add a visitor - should fail if this would produce a duplicate visitor`() {
      val request = createVisitorRequest()
      val expectedException = EntityInUseException("The person ID ${request.personId} or offenderVisitVisitorId ${request.offenderVisitVisitorId}) is already on the visit $officialVisitId and cannot be added again")

      whenever(syncOfficialVisitorService.createVisitor(officialVisitId, request)).thenThrow(expectedException)

      val exception = assertThrows<EntityInUseException> {
        facade.createOfficialVisitor(officialVisitId, request)
      }

      assertThat(exception.message).isEqualTo(expectedException.message)

      verify(syncOfficialVisitorService).createVisitor(officialVisitId, request)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `remove a visitor - should remove a visitor from a visit and raise an event`() {
      val response = SyncRemoveVisitorResponse(
        officialVisitId = officialVisitId,
        officialVisitorId = officialVisitorId,
        prisonCode = MOORLAND,
        prisonerNumber = MOORLAND_PRISONER.number,
        contactId = contactId,
      )

      whenever(syncOfficialVisitorService.deleteVisitor(officialVisitId, officialVisitorId)).thenReturn(response)

      facade.deleteOfficialVisitor(officialVisitId, officialVisitorId)

      verify(syncOfficialVisitorService).deleteVisitor(officialVisitId, officialVisitorId)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISITOR_DELETED,
        prisonCode = MOORLAND,
        source = Source.NOMIS,
        identifier = officialVisitId,
        secondIdentifier = officialVisitorId,
        contactId = contactId,
        username = "NOMIS",
      )
    }

    @Test
    fun `remove a visitor - should silently succeed if the visit or visitor does not exist`() {
      whenever(syncOfficialVisitorService.deleteVisitor(officialVisitId, officialVisitorId)).thenReturn(null)

      facade.deleteOfficialVisitor(officialVisitId, officialVisitorId)

      verify(syncOfficialVisitorService).deleteVisitor(officialVisitId, officialVisitorId)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update a visitor - should update a visitor and raise an event`() {
      val request = updateVisitorRequest()
      val mockedResponse = updateVisitorResponse()
      whenever(syncOfficialVisitorService.updateVisitor(officialVisitId, officialVisitorId, request)).thenReturn(
        mockedResponse,
      )

      val response = facade.updateOfficialVisitor(officialVisitId, officialVisitorId, request)

      assertThat(response.officialVisitorId).isEqualTo(officialVisitorId)

      verify(syncOfficialVisitorService).updateVisitor(officialVisitId, officialVisitorId, request)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.VISITOR_UPDATED,
        prisonCode = MOORLAND,
        source = Source.NOMIS,
        identifier = officialVisitId,
        secondIdentifier = officialVisitorId,
        contactId = contactId,
        username = MOORLAND_PRISON_USER.username,
      )
    }

    @Test
    fun `update a visitor - should fail if the visit was not found`() {
      val request = updateVisitorRequest()
      val expectedException = EntityNotFoundException("The official visit with id $officialVisitId was not found")

      whenever(syncOfficialVisitorService.updateVisitor(officialVisitId, officialVisitorId, request)).thenThrow(expectedException)

      val exception = assertThrows<EntityNotFoundException> {
        facade.updateOfficialVisitor(officialVisitId, officialVisitorId, request)
      }

      assertThat(exception.message).isEqualTo(expectedException.message)

      verify(syncOfficialVisitorService).updateVisitor(officialVisitId, officialVisitorId, request)
      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `update a visitor - should fail if the visitor was not found`() {
      val request = updateVisitorRequest()
      val expectedException = EntityNotFoundException("The official visitor with id $officialVisitorId was not found")

      whenever(syncOfficialVisitorService.updateVisitor(officialVisitId, officialVisitorId, request)).thenThrow(expectedException)

      val exception = assertThrows<EntityNotFoundException> {
        facade.updateOfficialVisitor(officialVisitId, officialVisitorId, request)
      }

      assertThat(exception.message).isEqualTo(expectedException.message)

      verify(syncOfficialVisitorService).updateVisitor(officialVisitId, officialVisitorId, request)
      verifyNoInteractions(outboundEventsService)
    }

    private fun createVisitorRequest() = SyncCreateOfficialVisitorRequest(
      offenderVisitVisitorId = 9L,
      personId = contactId,
      firstName = "First",
      lastName = "Last",
      relationshipTypeCode = RelationshipType.OFFICIAL,
      relationshipToPrisoner = "POL",
      createUsername = MOORLAND_PRISON_USER.username,
      createDateTime = creationTime,
    )

    private fun createVisitorResponse() = SyncAddVisitorResponse(
      officialVisitId = officialVisitId,
      officialVisitorId = officialVisitorId,
      prisonCode = MOORLAND,
      prisonerNumber = MOORLAND_PRISONER.number,
      visitor = SyncOfficialVisitor(
        officialVisitorId = officialVisitorId,
        contactId = contactId,
        firstName = "First",
        lastName = "Last",
        createdBy = MOORLAND_PRISON_USER.username,
        createdTime = creationTime,
      ),
    )

    private fun updateVisitorRequest() = SyncUpdateOfficialVisitorRequest(
      offenderVisitVisitorId = 9L,
      personId = contactId,
      firstName = "First",
      lastName = "Last",
      relationshipTypeCode = RelationshipType.OFFICIAL,
      relationshipToPrisoner = "POL",
      updateUsername = MOORLAND_PRISON_USER.username,
      updateDateTime = updateTime,
    )

    private fun updateVisitorResponse() = SyncUpdateVisitorResponse(
      officialVisitId = officialVisitId,
      officialVisitorId = officialVisitorId,
      prisonCode = MOORLAND,
      prisonerNumber = MOORLAND_PRISONER.number,
      visitor = SyncOfficialVisitor(
        officialVisitorId = officialVisitorId,
        contactId = contactId,
        firstName = "First",
        lastName = "Last",
        createdBy = MOORLAND_PRISON_USER.username,
        createdTime = creationTime,
        updatedBy = MOORLAND_PRISON_USER.username,
        updatedTime = updateTime,
      ),
    )
  }
}
