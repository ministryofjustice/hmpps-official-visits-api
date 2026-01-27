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
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncVisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class SyncVisitSlotServiceTest {
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()

  private val syncVisitSlotService = SyncVisitSlotService(prisonVisitSlotRepository, prisonTimeSlotRepository)

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @AfterEach
  fun afterEach() {
    reset(prisonVisitSlotRepository, prisonTimeSlotRepository)
  }

  @Test
  fun `should get a visit slot by ID`() {
    val visitSlotEntity = prisonVisitSlotEntity(1L)
    val timeSlotEntity = prisonTimeSlotEntity(1L)

    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlotEntity))

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(visitSlotEntity))

    val timeSlot = syncVisitSlotService.getPrisonVisitSlotById(1L)
    visitSlotEntity.assertWithResponse(timeSlot)

    verify(prisonVisitSlotRepository).findById(1L)
  }

  @Test
  fun `should fail to get a visit slot by ID`() {
    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      syncVisitSlotService.getPrisonVisitSlotById(1L)
    }

    verify(prisonVisitSlotRepository).findById(1L)
  }

  @Test
  fun `should create a visit slot and return it`() {
    val request = createVisitSlotRequest()
    val timeSlotEntity = prisonTimeSlotEntity(1L)
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlotEntity))
    whenever(prisonVisitSlotRepository.saveAndFlush(request.toEntity())).thenReturn(request.toEntity())

    val created = syncVisitSlotService.createPrisonVisitSlot(request)

    val visitSlotCaptor = argumentCaptor<PrisonVisitSlotEntity>()
    verify(prisonVisitSlotRepository).saveAndFlush(visitSlotCaptor.capture())
    visitSlotCaptor.firstValue.assertWithResponse(created)
    created.assertWithCreateRequest(request)
    verify(prisonTimeSlotRepository).findById(1L)
    verify(prisonVisitSlotRepository).saveAndFlush(any())
  }

  @Test
  fun `should fail to create a visit slot and throw an exception`() {
    val request = createVisitSlotRequest()
    val timeSlotEntity = prisonTimeSlotEntity(1L)
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlotEntity))

    whenever(prisonVisitSlotRepository.saveAndFlush(request.toEntity())).thenThrow(RuntimeException("Bang!"))

    assertThrows<RuntimeException> {
      syncVisitSlotService.createPrisonVisitSlot(request)
    }
    verify(prisonTimeSlotRepository).findById(1L)
    verify(prisonVisitSlotRepository).saveAndFlush(any())
  }

  @Test
  fun `should update a visit slot and return it`() {
    val request = updateVisitSlotRequest()
    val timeSlotEntity = prisonTimeSlotEntity(1L)

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(request.toEntity()))
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlotEntity))

    whenever(prisonVisitSlotRepository.saveAndFlush(any())).thenReturn(request.toEntity())

    val updated = syncVisitSlotService.updatePrisonVisitSlot(1L, request)

    val visitSlotCaptor = argumentCaptor<PrisonVisitSlotEntity>()
    verify(prisonVisitSlotRepository).saveAndFlush(visitSlotCaptor.capture())

    visitSlotCaptor.firstValue.assertWithResponse(updated)
    verify(prisonTimeSlotRepository).findById(1L)
    verify(prisonVisitSlotRepository).findById(1L)
  }

  @Test
  fun `should fail to update visit slot which does not exist`() {
    val updateRequest = updateVisitSlotRequest()
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.empty())
    assertThrows<EntityNotFoundException> {
      syncVisitSlotService.updatePrisonVisitSlot(1L, updateRequest)
    }
    verify(prisonVisitSlotRepository).findById(1L)
    verifyNoMoreInteractions(prisonVisitSlotRepository)
  }

  private fun PrisonVisitSlotEntity.assertWithResponse(model: SyncVisitSlot) {
    assertThat(prisonVisitSlotId).isEqualTo(model.visitSlotId)
    assertThat(prisonTimeSlotId).isEqualTo(model.prisonTimeSlotId)
    assertThat(dpsLocationId).isEqualTo(model.dpsLocationId)
    assertThat(maxAdults).isEqualTo(model.maxAdults)
    assertThat(createdBy).isEqualTo(model.createdBy)
    assertThat(createdTime).isEqualTo(model.createdTime)
    assertThat(updatedTime).isEqualTo(model.updatedTime)
    assertThat(updatedBy).isEqualTo(model.updatedBy)
  }

  private fun SyncVisitSlot.assertWithCreateRequest(request: SyncCreateVisitSlotRequest) {
    assertThat(prisonTimeSlotId).isEqualTo(request.prisonTimeSlotId)
    assertThat(dpsLocationId).isEqualTo(request.dpsLocationId)
    assertThat(maxAdults).isEqualTo(request.maxAdults)
    assertThat(createdBy).isEqualTo(request.createdBy)
    assertThat(createdTime).isEqualTo(request.createdTime)
  }

  private fun prisonVisitSlotEntity(prisonVisitSlotId: Long = 1L) = PrisonVisitSlotEntity(
    prisonVisitSlotId = prisonVisitSlotId,
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    createdBy = "Test",
    createdTime = createdTime,
  )

  private fun createVisitSlotRequest() = SyncCreateVisitSlotRequest(
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    createdBy = "Test",
    createdTime = createdTime,
  )

  private fun updateVisitSlotRequest() = SyncUpdateVisitSlotRequest(
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    updatedBy = "Test",
    updatedTime = updatedTime,
  )

  fun SyncUpdateVisitSlotRequest.toEntity() = PrisonVisitSlotEntity(
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    createdBy = "Test",
    createdTime = createdTime,
    updatedBy = this.updatedBy,
    updatedTime = updatedTime,
  )

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
}
