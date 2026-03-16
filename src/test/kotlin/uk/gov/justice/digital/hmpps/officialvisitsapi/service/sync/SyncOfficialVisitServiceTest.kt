package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAPrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OfficialVisitMetricTelemetryService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitMetricInfo
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class SyncOfficialVisitServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val officialVisitorRepository: OfficialVisitorRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val officialVisitMetricTelemetryService: OfficialVisitMetricTelemetryService = mock()

  private val createdTime = LocalDateTime.now().minusDays(2)

  private val syncOfficialVisitService = SyncOfficialVisitService(
    officialVisitRepository,
    officialVisitorRepository,
    prisonerVisitedRepository,
    prisonVisitSlotRepository,
    officialVisitMetricTelemetryService,
  )

  @AfterEach
  fun afterEach() {
    reset(officialVisitRepository, officialVisitorRepository, prisonerVisitedRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `get a visit - should return an official visit by ID`() {
    val visitEntity = createAVisitEntity(1L)
    val prisonerVisitedEntity = createAPrisonerVisitedEntity(visitEntity, 2L)

    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.of(visitEntity))
    whenever(prisonerVisitedRepository.findByOfficialVisit(visitEntity)).thenReturn(prisonerVisitedEntity)

    val visit = syncOfficialVisitService.getVisitById(1L)
    visitEntity.assertWithResponse(visit)

    verify(officialVisitRepository).findById(1L)
    verify(prisonerVisitedRepository).findByOfficialVisit(visitEntity)
  }

  @Test
  fun `get a visit - should fail if the visit does not exist`() {
    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.getVisitById(1L)
    }

    verify(officialVisitRepository).findById(1L)
  }

  @Test
  fun `get a visit - should fail if the prisoner visited does not exist`() {
    val visitEntity = createAVisitEntity(1L)

    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.of(visitEntity))
    whenever(prisonerVisitedRepository.findByOfficialVisit(visitEntity)).thenReturn(null)

    assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.getVisitById(1L)
    }

    verify(officialVisitRepository).findById(1L)
    verify(prisonerVisitedRepository).findByOfficialVisit(visitEntity)
  }

  @Test
  fun `create a visit - should succeed`() {
    val visitSlotEntity = prisonVisitSlotEntity(1L)
    val request = createVisitRequest(1L)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, request)
    val prisonerVisitedEntity = PrisonerVisitedEntity(
      officialVisit = officialVisitEntity,
      prisonerNumber = officialVisitEntity.prisonerNumber,
      createdBy = officialVisitEntity.createdBy,
      createdTime = officialVisitEntity.createdTime,
    )

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(visitSlotEntity))
    whenever(officialVisitRepository.findByOffenderVisitId(request.offenderVisitId!!)).thenReturn(null)
    whenever(officialVisitRepository.saveAndFlush(officialVisitEntity)).thenReturn(officialVisitEntity)
    whenever(prisonerVisitedRepository.saveAndFlush(prisonerVisitedEntity)).thenReturn(prisonerVisitedEntity)

    val result = syncOfficialVisitService.createVisit(request)

    with(result) {
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(prisonerNumber).isEqualTo(request.prisonerNumber)
      assertThat(visitDate).isEqualTo(request.visitDate)
      assertThat(startTime).isEqualTo(request.startTime)
      assertThat(endTime).isEqualTo(request.endTime)
      assertThat(dpsLocationId).isEqualTo(request.dpsLocationId)
    }

    verify(prisonVisitSlotRepository).findById(1L)
    verify(officialVisitRepository).findByOffenderVisitId(request.offenderVisitId!!)
    verify(officialVisitRepository).saveAndFlush(officialVisitEntity)
    verify(prisonerVisitedRepository).saveAndFlush(prisonerVisitedEntity)
    verify(officialVisitMetricTelemetryService, atLeastOnce()).send(
      eventType = MetricsEvents.CREATE,
      info = VisitMetricInfo(
        prisonerNumber = officialVisitEntity.prisonerNumber,
        startTime = officialVisitEntity.startTime,
        source = Source.NOMIS,
        username = result.createdBy,
        officialVisitId = 0,
        prisonCode = result.prisonCode,
        numberOfVisitors = result.visitors.size.toLong(),
        locationType = null,
      ),
    )
  }

  @Test
  fun `create a visit - should fail if the visit slot does not exist`() {
    val request = createVisitRequest(1L)

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.createVisit(request)
    }

    verify(prisonVisitSlotRepository).findById(1L)
    verifyNoInteractions(officialVisitRepository, prisonerVisitedRepository)
  }

  @Test
  fun `create a visit - should fail if there is an exception on insert`() {
    val visitSlotEntity = prisonVisitSlotEntity(1L)
    val request = createVisitRequest(1L)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, request)
    val prisonerVisitedEntity = PrisonerVisitedEntity(
      officialVisit = officialVisitEntity,
      prisonerNumber = officialVisitEntity.prisonerNumber,
      createdBy = officialVisitEntity.createdBy,
      createdTime = officialVisitEntity.createdTime,
    )

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(visitSlotEntity))
    whenever(officialVisitRepository.findByOffenderVisitId(request.offenderVisitId!!)).thenReturn(null)
    whenever(officialVisitRepository.saveAndFlush(officialVisitEntity)).thenReturn(officialVisitEntity)
    whenever(prisonerVisitedRepository.saveAndFlush(prisonerVisitedEntity)).thenThrow(RuntimeException("bang!"))

    assertThrows<RuntimeException> {
      syncOfficialVisitService.createVisit(request)
    }

    verify(prisonVisitSlotRepository).findById(1L)
    verify(officialVisitRepository).findByOffenderVisitId(request.offenderVisitId!!)
    verify(officialVisitRepository).saveAndFlush(officialVisitEntity)
    verify(prisonerVisitedRepository).saveAndFlush(prisonerVisitedEntity)
  }

  @Test
  fun `create a visit - should fail if a duplicate offender visit ID is present`() {
    val visitSlotEntity = prisonVisitSlotEntity(1L)
    val request = createVisitRequest(1L)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, request)

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(visitSlotEntity))
    whenever(officialVisitRepository.findByOffenderVisitId(request.offenderVisitId!!)).thenReturn(officialVisitEntity)

    val exception = assertThrows<EntityInUseException> {
      syncOfficialVisitService.createVisit(request)
    }

    assertThat(exception.message).isEqualTo(
      "Official visit with offenderVisitId ${request.offenderVisitId} already exists (DPS ID ${officialVisitEntity.officialVisitId})",
    )

    verify(prisonVisitSlotRepository).findById(1L)
    verify(officialVisitRepository).findByOffenderVisitId(request.offenderVisitId!!)
    verifyNoMoreInteractions(officialVisitRepository, prisonerVisitedRepository)
  }

  @Test
  fun `update a visit - should update an official visit`() {
    val officialVisitId = 1L
    val visitSlotId = 1L
    val visitSlotEntity = prisonVisitSlotEntity(visitSlotId)
    val createRequest = createVisitRequest(visitSlotId)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, createRequest)
    val prisonerVisitedEntity = PrisonerVisitedEntity(
      officialVisit = officialVisitEntity,
      prisonerNumber = officialVisitEntity.prisonerNumber,
      createdBy = officialVisitEntity.createdBy,
      createdTime = officialVisitEntity.createdTime,
    )

    val request = updateVisitRequest(visitSlotId)

    whenever(officialVisitRepository.findById(officialVisitId)).thenReturn(Optional.of(officialVisitEntity))
    whenever(prisonerVisitedRepository.findByOfficialVisit(officialVisitEntity)).thenReturn(prisonerVisitedEntity)
    whenever(officialVisitRepository.saveAndFlush(officialVisitEntity)).thenReturn(officialVisitEntity)

    syncOfficialVisitService.updateVisit(officialVisitId, request)

    verify(officialVisitRepository).findById(officialVisitId)
    verify(prisonerVisitedRepository).findByOfficialVisit(officialVisitEntity)
    verify(officialVisitRepository).saveAndFlush(officialVisitEntity)
    verify(officialVisitMetricTelemetryService, atLeastOnce()).send(
      eventType = MetricsEvents.AMEND,
      info = VisitMetricInfo(
        prisonerNumber = officialVisitEntity.prisonerNumber,
        startTime = officialVisitEntity.startTime,
        source = Source.NOMIS,
        username = request.updateUsername,
        officialVisitId = 0,
        prisonCode = request.prisonCode,
        locationType = null,
      ),
    )
  }

  @Test
  fun `update a visit - should fail when the visit is not found`() {
    val officialVisitId = 99L
    val visitSlotId = 1L
    val request = updateVisitRequest(visitSlotId)

    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.updateVisit(officialVisitId, request)
    }

    assertThat(exception.message).isEqualTo("Official visit with ID $officialVisitId not found")

    verify(officialVisitRepository).findById(officialVisitId)
    verifyNoInteractions(prisonerVisitedRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `delete a visit - should succeed`() {
    val visitEntity = createAVisitEntity(1L)

    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.of(visitEntity))

    val syncResponse = syncOfficialVisitService.deleteVisit(1L)

    assertThat(syncResponse?.officialVisitId).isEqualTo(visitEntity.officialVisitId)
    assertThat(syncResponse?.prisonerNumber).isEqualTo(visitEntity.prisonerNumber)
    assertThat(syncResponse?.prisonCode).isEqualTo(visitEntity.prisonCode)
    assertThat(syncResponse?.visitors).size().isEqualTo(2)

    assertThat(syncResponse?.visitors).extracting("officialVisitorId").containsAll(
      visitEntity.officialVisitors().map { it.officialVisitorId }.toSet(),
    )

    verify(officialVisitRepository).findById(1L)
    verify(prisonerVisitedRepository).deleteByOfficialVisit(visitEntity)
    verify(officialVisitorRepository).deleteByOfficialVisit(visitEntity)
    verify(officialVisitRepository).deleteById(1L)
  }

  @Test
  fun `delete a visit - should silently succeed when the official visit ID is not found`() {
    whenever(officialVisitRepository.findById(99L)).thenReturn(Optional.empty())
    syncOfficialVisitService.deleteVisit(99L)

    verify(officialVisitRepository).findById(99L)
    verifyNoInteractions(prisonerVisitedRepository, officialVisitorRepository)
  }

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
    createUsername = "Bob",
  )

  private fun updateVisitRequest(visitSlotId: Long) = SyncUpdateOfficialVisitRequest(
    offenderVisitId = 2L,
    prisonVisitSlotId = visitSlotId,
    offenderBookId = MOORLAND_PRISONER.bookingId,
    prisonCode = MOORLAND,
    prisonerNumber = MOORLAND_PRISONER.number,
    visitDate = tomorrow(),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    visitStatusCode = VisitStatusType.EXPIRED,
    commentText = "updated comment",
    visitorConcernText = "updated concern",
    visitOrderNumber = 5678,
    updateUsername = MOORLAND_PRISON_USER.username,
    updateDateTime = LocalDateTime.now().minusMinutes(5),
  )

  private fun prisonVisitSlotEntity(prisonVisitSlotId: Long = 1L) = PrisonVisitSlotEntity(
    prisonVisitSlotId = prisonVisitSlotId,
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    createdBy = "Test",
    createdTime = createdTime,
  )

  private fun OfficialVisitEntity.assertWithResponse(model: SyncOfficialVisit) {
    assertThat(officialVisitId).isEqualTo(model.officialVisitId)
    assertThat(prisonVisitSlot.prisonVisitSlotId).isEqualTo(model.prisonVisitSlotId)
    assertThat(prisonCode).isEqualTo(model.prisonCode)
    assertThat(visitDate).isEqualTo(model.visitDate)
    assertThat(startTime).isEqualTo(model.startTime)
    assertThat(endTime).isEqualTo(model.endTime)
    assertThat(dpsLocationId).isEqualTo(model.dpsLocationId)
    assertThat(visitTypeCode).isEqualTo(model.visitType)
    assertThat(visitStatusCode).isEqualTo(model.statusCode)
    assertThat(createdBy).isEqualTo(model.createdBy)
    assertThat(createdTime).isEqualTo(model.createdTime)
    assertThat(updatedTime).isEqualTo(model.updatedTime)
    assertThat(updatedBy).isEqualTo(model.updatedBy)

    assertThat(this.officialVisitors().size).isEqualTo(2)

    var index = 0

    this.officialVisitors().forEach { visitor ->
      assertThat(visitor.contactId).isEqualTo(model.visitors[index].contactId)
      assertThat(visitor.firstName).isEqualTo(model.visitors[index].firstName)
      assertThat(visitor.lastName).isEqualTo(model.visitors[index].lastName)
      assertThat(visitor.relationshipTypeCode).isEqualTo(model.visitors[index].relationshipType)
      assertThat(visitor.relationshipCode).isEqualTo(model.visitors[index].relationshipCode)
      assertThat(visitor.attendanceCode).isEqualTo(model.visitors[index].attendanceCode)
      assertThat(visitor.leadVisitor).isEqualTo(model.visitors[index].leadVisitor)
      assertThat(visitor.assistedVisit).isEqualTo(model.visitors[index].assistedVisit)
      assertThat(visitor.visitorNotes).isEqualTo(model.visitors[index].visitorNotes)
      index++
    }
  }
}
