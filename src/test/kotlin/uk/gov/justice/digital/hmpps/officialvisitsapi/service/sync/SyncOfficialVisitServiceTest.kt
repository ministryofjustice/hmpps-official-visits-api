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
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAPrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.util.Optional

class SyncOfficialVisitServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val officialVisitorRepository: OfficialVisitorRepository = mock()

  private val syncOfficialVisitService = SyncOfficialVisitService(officialVisitRepository, prisonerVisitedRepository, officialVisitorRepository)

  @AfterEach
  fun afterEach() {
    reset(officialVisitRepository, prisonerVisitedRepository)
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
  fun `should delete official visit by ID if exist`() {
    val visitEntity = createAVisitEntity(1L)

    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.of(visitEntity))
    val syncResponse = syncOfficialVisitService.deleteOfficialVisit(1L)
    assertThat(syncResponse?.officialVisitId).isEqualTo(visitEntity.officialVisitId)
    assertThat(syncResponse?.prisonerNumber).isEqualTo(visitEntity.prisonerNumber)
    assertThat(syncResponse?.prisonCode).isEqualTo(visitEntity.prisonCode)
    assertThat(syncResponse?.visitors).size().isEqualTo(2)
    var index = 0
    syncResponse?.visitors?.forEach { visitor ->
      assertThat(visitor.contactId).isEqualTo(syncResponse.visitors[index].contactId)
      assertThat(visitor.officialVisitorId).isEqualTo(syncResponse.visitors[index].officialVisitorId)
      index++
    }
    verify(officialVisitRepository).findById(1L)
    verify(prisonerVisitedRepository).deleteByOfficialVisit(visitEntity)
    verify(officialVisitorRepository).deleteByOfficialVisit(visitEntity)
    verify(officialVisitRepository).deleteById(1L)
  }

  @Test
  fun `should not return exception for non existent official visit passed`() {
    whenever(officialVisitRepository.findById(99L)).thenReturn(Optional.empty())
    syncOfficialVisitService.deleteOfficialVisit(99L)

    verify(officialVisitRepository).findById(99L)
    verifyNoInteractions(prisonerVisitedRepository, officialVisitorRepository)
  }

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
