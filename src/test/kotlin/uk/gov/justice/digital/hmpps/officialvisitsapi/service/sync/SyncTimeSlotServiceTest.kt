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
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class SyncTimeSlotServiceTest {
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()

  private val syncTimeSlotService = SyncTimeSlotService(prisonTimeSlotRepository)

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @AfterEach
  fun afterEach() {
    reset(prisonTimeSlotRepository)
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
}
