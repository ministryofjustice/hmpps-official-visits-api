package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAPrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class SyncOfficialVisitServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val officialVisitorRepository: OfficialVisitorRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val contactsService: ContactsService = mock()

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  private val syncOfficialVisitService = SyncOfficialVisitService(
    officialVisitRepository,
    officialVisitorRepository,
    prisonerVisitedRepository,
    prisonVisitSlotRepository,
    contactsService,
  )

  @AfterEach
  fun afterEach() {
    reset(officialVisitRepository, prisonerVisitedRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `should return an official visit by ID for sync`() {
    val visitEntity = createAVisitEntity(1L)
    val prisonerVisitedEntity = createAPrisonerVisitedEntity(visitEntity, 2L)

    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.of(visitEntity))
    whenever(prisonerVisitedRepository.findByOfficialVisit(visitEntity)).thenReturn(prisonerVisitedEntity)

    val visit = syncOfficialVisitService.getOfficialVisitById(1L)
    visitEntity.assertWithResponse(visit)

    verify(officialVisitRepository).findById(1L)
    verify(prisonerVisitedRepository).findByOfficialVisit(visitEntity)
  }

  @Test
  fun `should fail to get an official visit by ID if it does not exist`() {
    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.getOfficialVisitById(1L)
    }

    verify(officialVisitRepository).findById(1L)
  }

  @Test
  fun `should fail to get an official visit by ID if the prisoner visited does not exist`() {
    val visitEntity = createAVisitEntity(1L)

    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.of(visitEntity))
    whenever(prisonerVisitedRepository.findByOfficialVisit(visitEntity)).thenReturn(null)

    assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.getOfficialVisitById(1L)
    }

    verify(officialVisitRepository).findById(1L)
    verify(prisonerVisitedRepository).findByOfficialVisit(visitEntity)
  }

  @Test
  fun `should create an official visit`() {
    val visitSlotEntity = prisonVisitSlotEntity(1L)
    val request = createOfficialVisitRequest(1L)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, request)
    val prisonerVisitedEntity = PrisonerVisitedEntity(
      officialVisit = officialVisitEntity,
      prisonerNumber = officialVisitEntity.prisonerNumber,
      createdBy = officialVisitEntity.createdBy,
      createdTime = officialVisitEntity.createdTime,
    )

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(visitSlotEntity))
    whenever(officialVisitRepository.saveAndFlush(officialVisitEntity)).thenReturn(officialVisitEntity)
    whenever(prisonerVisitedRepository.saveAndFlush(prisonerVisitedEntity)).thenReturn(prisonerVisitedEntity)

    val result = syncOfficialVisitService.createOfficialVisit(request)

    with(result) {
      assertThat(this.prisonCode).isEqualTo(request.prisonCode)
      assertThat(this.prisonerNumber).isEqualTo(request.prisonerNumber)
      assertThat(this.visitDate).isEqualTo(request.visitDate)
      assertThat(this.startTime).isEqualTo(request.startTime)
      assertThat(this.endTime).isEqualTo(request.endTime)
      assertThat(this.dpsLocationId).isEqualTo(request.dpsLocationId)
    }

    verify(prisonVisitSlotRepository).findById(1L)
    verify(officialVisitRepository).saveAndFlush(officialVisitEntity)
    verify(prisonerVisitedRepository).saveAndFlush(prisonerVisitedEntity)
  }

  @Test
  fun `should fail to create an official visit - references a visit slot that does not exist`() {
    val request = createOfficialVisitRequest(1L)

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.createOfficialVisit(request)
    }

    verify(prisonVisitSlotRepository).findById(1L)
    verifyNoInteractions(officialVisitRepository, prisonerVisitedRepository)
  }

  @Test
  fun `should fail to create an official visit - exception on insert`() {
    val visitSlotEntity = prisonVisitSlotEntity(1L)
    val request = createOfficialVisitRequest(1L)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, request)
    val prisonerVisitedEntity = PrisonerVisitedEntity(
      officialVisit = officialVisitEntity,
      prisonerNumber = officialVisitEntity.prisonerNumber,
      createdBy = officialVisitEntity.createdBy,
      createdTime = officialVisitEntity.createdTime,
    )

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(visitSlotEntity))
    whenever(officialVisitRepository.saveAndFlush(officialVisitEntity)).thenReturn(officialVisitEntity)
    whenever(prisonerVisitedRepository.saveAndFlush(prisonerVisitedEntity)).thenThrow(RuntimeException("bang!"))

    assertThrows<RuntimeException> {
      syncOfficialVisitService.createOfficialVisit(request)
    }

    verify(prisonVisitSlotRepository).findById(1L)
    verify(officialVisitRepository).saveAndFlush(officialVisitEntity)
    verify(prisonerVisitedRepository).saveAndFlush(prisonerVisitedEntity)
  }

  private fun createOfficialVisitRequest(visitSlotId: Long) = SyncCreateOfficialVisitRequest(
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
