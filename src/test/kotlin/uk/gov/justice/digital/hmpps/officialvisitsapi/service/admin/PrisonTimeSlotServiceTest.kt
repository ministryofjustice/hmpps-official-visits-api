package uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin

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
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class PrisonTimeSlotServiceTest {
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val timeSlotService = PrisonTimeSlotService(prisonTimeSlotRepository, prisonVisitSlotRepository)

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

    val timeSlot = timeSlotService.getPrisonTimeSlotById(1L)
    timeSlotEntity.assertWithResponse(timeSlot)

    verify(prisonTimeSlotRepository).findById(1L)
  }

  @Test
  fun `should fail to get a time slot by ID`() {
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      timeSlotService.getPrisonTimeSlotById(1L)
    }

    verify(prisonTimeSlotRepository).findById(1L)
  }

  @Test
  fun `should create a time slot and return it`() {
    val request = createTimeSlotRequest()
    whenever(prisonTimeSlotRepository.saveAndFlush(any())).thenReturn(request.toEntity(MOORLAND_PRISON_USER.username))

    val created = timeSlotService.create(request, MOORLAND_PRISON_USER)

    val timeSlotCaptor = argumentCaptor<PrisonTimeSlotEntity>()
    verify(prisonTimeSlotRepository).saveAndFlush(timeSlotCaptor.capture())
    timeSlotCaptor.firstValue.assertWithResponse(created)
    created.assertWithCreateRequest(request)

    verify(prisonTimeSlotRepository).saveAndFlush(any())
  }

  @Test
  fun `should fail to create a time slot and throw an exception`() {
    val request = createTimeSlotRequest()
    whenever(prisonTimeSlotRepository.saveAndFlush(request.toEntity(MOORLAND_PRISON_USER.username))).thenThrow(RuntimeException("Bang!"))

    assertThrows<RuntimeException> {
      timeSlotService.create(request, MOORLAND_PRISON_USER)
    }

    verify(prisonTimeSlotRepository).saveAndFlush(any())
  }

  @Test
  fun `should update a time slot and return it`() {
    val request = updateTimeSlotRequest()

    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(prisonTimeSlotEntity(1L)))
    whenever(prisonTimeSlotRepository.saveAndFlush(any())).thenReturn(prisonTimeSlotEntity(1L))

    val updated = timeSlotService.update(prisonTimeSlotId = 1L, request = request, user = MOORLAND_PRISON_USER)

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
      timeSlotService.update(1L, updateRequest, MOORLAND_PRISON_USER)
    }
    verify(prisonTimeSlotRepository).findById(1L)
    verifyNoMoreInteractions(prisonTimeSlotRepository)
  }

  @Test
  fun `should fail to delete time slot if it does not exist`() {
    val expectedException = EntityNotFoundException("Prison time slot with ID 99 was not found")

    whenever(prisonTimeSlotRepository.findById(99L)).thenThrow(expectedException)
    val exception = assertThrows<EntityNotFoundException> {
      timeSlotService.delete(99L)
    }
    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(prisonTimeSlotRepository).findById(99L)
    verifyNoMoreInteractions(prisonTimeSlotRepository)
  }

  @Test
  fun `should delete time slot when there is no associated visit slot exists`() {
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(prisonTimeSlotEntity(1L)))
    whenever(prisonVisitSlotRepository.existsByPrisonTimeSlotId(1L)).thenReturn(false)
    timeSlotService.delete(1L)
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
      timeSlotService.delete(1L)
    }
    val expectedException = EntityInUseException("The prison time slot has one or more visit slots associated with it and cannot be deleted.")
    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(prisonTimeSlotRepository).findById(1L)
    verify(prisonVisitSlotRepository).existsByPrisonTimeSlotId(1L)
    verifyNoMoreInteractions(prisonTimeSlotRepository)
  }

  private fun PrisonTimeSlotEntity.assertWithResponse(model: TimeSlot) {
    assertThat(prisonTimeSlotId).isEqualTo(model.prisonTimeSlotId)
    assertThat(prisonCode).isEqualTo(model.prisonCode)
    assertThat(dayCode).isEqualTo(model.dayCode)
    assertThat(startTime).isEqualTo(model.startTime)
    assertThat(endTime).isEqualTo(model.endTime)
    assertThat(effectiveDate).isEqualTo(model.effectiveDate)
    assertThat(expiryDate).isEqualTo(model.expiryDate)
    assertThat(createdBy).isEqualTo(model.createdBy)
  }

  private fun TimeSlot.assertWithCreateRequest(request: CreateTimeSlotRequest) {
    assertThat(prisonCode).isEqualTo(request.prisonCode)
    assertThat(dayCode).isEqualTo(request.dayCode)
    assertThat(startTime).isEqualTo(request.startTime)
    assertThat(endTime).isEqualTo(request.endTime)
    assertThat(effectiveDate).isEqualTo(request.effectiveDate)
    assertThat(expiryDate).isEqualTo(request.expiryDate)
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

  private fun createTimeSlotRequest() = CreateTimeSlotRequest(
    prisonCode = "MDI",
    dayCode = DayType.MON,
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    effectiveDate = LocalDate.now().plusDays(1),
    expiryDate = LocalDate.now().plusDays(365),
  )

  private fun updateTimeSlotRequest() = UpdateTimeSlotRequest(
    prisonCode = "MDI",
    dayCode = DayType.MON,
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    effectiveDate = LocalDate.now().plusDays(1),
    expiryDate = LocalDate.now().plusDays(365),
  )

  fun UpdateTimeSlotRequest.toEntity() = PrisonTimeSlotEntity(
    prisonCode = this.prisonCode,
    dayCode = this.dayCode,
    startTime = this.startTime,
    endTime = this.endTime,
    effectiveDate = this.effectiveDate,
    expiryDate = this.expiryDate,
    createdBy = "Test",
    createdTime = createdTime,
  )
}
