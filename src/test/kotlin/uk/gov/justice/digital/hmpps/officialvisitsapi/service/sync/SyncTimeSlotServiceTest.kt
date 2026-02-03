package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class SyncTimeSlotServiceTest {
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val syncTimeSlotService = SyncTimeSlotService(prisonTimeSlotRepository, prisonVisitSlotRepository)

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @AfterEach
  fun afterEach() {
    reset(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `should get a time slot by ID`() {
    val timeSlotEntity = prisonTimeSlotEntity(1L)
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlotEntity))

    val timeSlot = syncTimeSlotService.getPrisonTimeSlotById(1L)
    timeSlotEntity.assertWithResponse(timeSlot)

    verify(prisonTimeSlotRepository).findById(1L)
  }

  @Test
  fun `should fail to get a time slot by ID`() {
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      syncTimeSlotService.getPrisonTimeSlotById(1L)
    }

    verify(prisonTimeSlotRepository).findById(1L)
  }

  @Test
  fun `should create a time slot and return it`() {
    val request = createTimeSlotRequest()
    whenever(prisonTimeSlotRepository.saveAndFlush(request.toEntity())).thenReturn(request.toEntity())

    val created = syncTimeSlotService.createPrisonTimeSlot(request)

    val timeSlotCaptor = argumentCaptor<PrisonTimeSlotEntity>()
    verify(prisonTimeSlotRepository).saveAndFlush(timeSlotCaptor.capture())
    timeSlotCaptor.firstValue.assertWithResponse(created)
    created.assertWithCreateRequest(request)

    verify(prisonTimeSlotRepository).saveAndFlush(any())
  }

  @Test
  fun `should fail to create a time slot and throw an exception`() {
    val request = createTimeSlotRequest()
    whenever(prisonTimeSlotRepository.saveAndFlush(request.toEntity())).thenThrow(RuntimeException("Bang!"))

    assertThrows<RuntimeException> {
      syncTimeSlotService.createPrisonTimeSlot(request)
    }

    verify(prisonTimeSlotRepository).saveAndFlush(any())
  }

  @Test
  fun `should update a time slot and return it`() {
    val request = updateTimeSlotRequest()

    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(request.toEntity()))
    whenever(prisonTimeSlotRepository.saveAndFlush(any())).thenReturn(request.toEntity())

    val updated = syncTimeSlotService.updatePrisonTimeSlot(1L, request)

    val timeSlotCaptor = argumentCaptor<PrisonTimeSlotEntity>()
    verify(prisonTimeSlotRepository).saveAndFlush(timeSlotCaptor.capture())

    timeSlotCaptor.firstValue.assertWithResponse(updated)
    verify(prisonTimeSlotRepository).findById(1L)
  }

  @Test
  fun `should fail to update time slot which does not exist`() {
    val updateRequest = updateTimeSlotRequest()
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.empty())
    assertThrows<EntityNotFoundException> {
      syncTimeSlotService.updatePrisonTimeSlot(1L, updateRequest)
    }
    verify(prisonTimeSlotRepository).findById(1L)
    verifyNoMoreInteractions(prisonTimeSlotRepository)
  }

  @Test
  fun `should silently accept a delete for a time slot which does not exist`() {
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.empty())

    val result = syncTimeSlotService.deletePrisonTimeSlot(1L)

    assertThat(result).isNull()
    verify(prisonTimeSlotRepository).findById(1L)
    verifyNoMoreInteractions(prisonTimeSlotRepository)
  }

  @Test
  fun `should delete time slot when there is no associated visit slot exists`() {
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(prisonTimeSlotEntity(1L)))
    whenever(prisonVisitSlotRepository.existsByPrisonTimeSlotId(1L)).thenReturn(false)
    syncTimeSlotService.deletePrisonTimeSlot(1L)
    verify(prisonTimeSlotRepository).deleteById(1L)

    verify(prisonTimeSlotRepository).findById(1L)
    verify(prisonVisitSlotRepository).existsByPrisonTimeSlotId(1L)
    verify(prisonTimeSlotRepository).deleteById(1L)

    verifyNoMoreInteractions(prisonTimeSlotRepository)
  }

  @Test
  fun `should fail to delete time slot when there is associated visit slot exists`() {
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(prisonTimeSlotEntity(1L)))
    whenever(prisonVisitSlotRepository.existsByPrisonTimeSlotId(1L)).thenReturn(true)
    val exception = assertThrows<EntityInUseException> {
      syncTimeSlotService.deletePrisonTimeSlot(1L)
    }
    val expectedException = EntityInUseException("The prison time slot has one or more visit slots associated with it and cannot be deleted.")
    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(prisonTimeSlotRepository).findById(1L)
    verify(prisonVisitSlotRepository).existsByPrisonTimeSlotId(1L)
    verifyNoMoreInteractions(prisonTimeSlotRepository)
  }

  @Test
  fun `should return summary of active time slots and associated visit slots for the prison code`() {
    val request = prisonVisitSlotEntity(1L)
    val timeSlotEntity = prisonTimeSlotEntity(1L)
    whenever(prisonTimeSlotRepository.findAllActiveByPrisonCode("MDI")).thenReturn(listOf(timeSlotEntity))
    whenever(prisonVisitSlotRepository.findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))).thenReturn(listOf(request))
    syncTimeSlotService.getAllPrisonTimeSlotsAndAssociatedVisitSlot("MDI", true)

    verify(prisonTimeSlotRepository).findAllActiveByPrisonCode("MDI")
    verify(prisonVisitSlotRepository).findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))
    verifyNoMoreInteractions(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `should return summary of all time slots and associated visit slots for the prison code`() {
    val request = prisonVisitSlotEntity(1L)
    val timeSlotEntity = prisonTimeSlotEntity(1L)
    whenever(prisonTimeSlotRepository.findAllByPrisonCode("MDI")).thenReturn(listOf(timeSlotEntity))
    whenever(prisonVisitSlotRepository.findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))).thenReturn(listOf(request))
    syncTimeSlotService.getAllPrisonTimeSlotsAndAssociatedVisitSlot("MDI", false)

    verify(prisonTimeSlotRepository).findAllByPrisonCode("MDI")
    verify(prisonVisitSlotRepository).findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))
    verifyNoMoreInteractions(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `should return empty summary if there are no timeslots associated with the prison code`() {
    val timeSlotEntity = prisonTimeSlotEntity(1L)
    whenever(prisonTimeSlotRepository.findAllByPrisonCode("MDIN")).thenReturn(listOf(timeSlotEntity))
    syncTimeSlotService.getAllPrisonTimeSlotsAndAssociatedVisitSlot("MDIN", true)

    verify(prisonTimeSlotRepository).findAllActiveByPrisonCode("MDIN")
    verifyNoMoreInteractions(prisonTimeSlotRepository)
  }

  private fun PrisonTimeSlotEntity.assertWithResponse(model: SyncTimeSlot) {
    assertThat(prisonTimeSlotId).isEqualTo(model.prisonTimeSlotId)
    assertThat(prisonCode).isEqualTo(model.prisonCode)
    assertThat(dayCode).isEqualTo(model.dayCode)
    assertThat(startTime).isEqualTo(model.startTime)
    assertThat(endTime).isEqualTo(model.endTime)
    assertThat(effectiveDate).isEqualTo(model.effectiveDate)
    assertThat(expiryDate).isEqualTo(model.expiryDate)
    assertThat(createdBy).isEqualTo(model.createdBy)
    assertThat(createdTime).isEqualTo(model.createdTime)
    assertThat(updatedTime).isEqualTo(model.updatedTime)
    assertThat(updatedBy).isEqualTo(model.updatedBy)
  }

  private fun SyncTimeSlot.assertWithCreateRequest(request: SyncCreateTimeSlotRequest) {
    assertThat(prisonCode).isEqualTo(request.prisonCode)
    assertThat(dayCode).isEqualTo(request.dayCode)
    assertThat(startTime).isEqualTo(request.startTime)
    assertThat(endTime).isEqualTo(request.endTime)
    assertThat(effectiveDate).isEqualTo(request.effectiveDate)
    assertThat(expiryDate).isEqualTo(request.expiryDate)
    assertThat(createdBy).isEqualTo(request.createdBy)
    assertThat(createdTime).isEqualTo(request.createdTime)
  }

  private fun prisonTimeSlotEntity(prisonTimeSlotId: Long = 1L) = PrisonTimeSlotEntity(
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

  fun SyncUpdateTimeSlotRequest.toEntity() = PrisonTimeSlotEntity(
    prisonCode = this.prisonCode,
    dayCode = this.dayCode,
    startTime = this.startTime,
    endTime = this.endTime,
    effectiveDate = this.effectiveDate,
    expiryDate = this.expiryDate,
    createdBy = "Test",
    createdTime = createdTime,
    updatedBy = this.updatedBy,
    updatedTime = this.updatedTime,
  )

  private fun prisonVisitSlotEntity(prisonVisitSlotId: Long = 1L) = PrisonVisitSlotEntity(
    prisonVisitSlotId = prisonVisitSlotId,
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    createdBy = "Test",
    createdTime = createdTime,
  )
}
