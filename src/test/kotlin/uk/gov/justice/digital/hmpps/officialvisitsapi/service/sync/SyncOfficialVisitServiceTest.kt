package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID
import kotlin.Long

class SyncOfficialVisitServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()

  private val syncOfficialVisitService = SyncOfficialVisitService(officialVisitRepository, prisonerVisitedRepository)

  private val createdTime = LocalDateTime.now().minusDays(2)

  @AfterEach
  fun afterEach() {
    reset(officialVisitRepository, prisonerVisitedRepository)
  }

  @Test
  fun `should return an official visit by ID for sync`() {
    val visitEntity = visitEntity(1L)
    val prisonerVisitedEntity = prisonerVisitedEntity(visitEntity, 2L)

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
    val visitEntity = visitEntity(1L)

    whenever(officialVisitRepository.findById(1L)).thenReturn(Optional.of(visitEntity))
    whenever(prisonerVisitedRepository.findByOfficialVisit(visitEntity)).thenReturn(null)

    assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.getOfficialVisitById(1L)
    }

    verify(officialVisitRepository).findById(1L)
    verify(prisonerVisitedRepository).findByOfficialVisit(visitEntity)
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

    // Check visitor information
  }

  private fun visitEntity(officialVisitId: Long = 1L) = OfficialVisitEntity(
    officialVisitId = officialVisitId,
    prisonCode = "MDI",
    prisonerNumber = "A1234AA",
    visitTypeCode = VisitType.IN_PERSON,
    visitDate = LocalDate.now().plusDays(1),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    createdBy = "Test",
    createdTime = createdTime,
    prisonVisitSlot = PrisonVisitSlotEntity(
      prisonVisitSlotId = 1L,
      prisonTimeSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
      maxAdults = 3,
      maxGroups = 3,
      maxVideoSessions = 3,
      createdBy = "Test",
      createdTime = createdTime,
    ),
  )

  private fun prisonerVisitedEntity(officialVisitEntity: OfficialVisitEntity, prisonerVisitedId: Long) = PrisonerVisitedEntity(
    prisonerVisitedId = prisonerVisitedId,
    officialVisit = officialVisitEntity,
    prisonerNumber = officialVisitEntity.prisonerNumber,
    attendanceCode = AttendanceType.ATTENDED,
    createdBy = "Test",
    createdTime = createdTime,
  )
}
