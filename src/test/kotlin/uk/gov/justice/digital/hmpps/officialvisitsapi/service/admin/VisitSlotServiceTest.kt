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
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class VisitSlotServiceTest {
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()
  private val officialVisitRepository: OfficialVisitRepository = mock()

  private val service = VisitSlotService(prisonVisitSlotRepository, prisonTimeSlotRepository, officialVisitRepository)

  private val createdTime = LocalDateTime.now().minusDays(2)

  @AfterEach
  fun afterEach() {
    reset(prisonVisitSlotRepository, prisonTimeSlotRepository, officialVisitRepository)
  }

  @Test
  fun `should get a visit slot by ID`() {
    val visitSlotEntity = prisonVisitSlotEntity()
    val timeSlotEntity = prisonTimeSlotEntity()

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(visitSlotEntity))
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlotEntity))

    val model = service.getById(1L)

    visitSlotEntity.assertWithResponse(model)

    verify(prisonVisitSlotRepository).findById(1L)
  }

  @Test
  fun `should fail to get a visit slot by ID`() {
    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      service.getById(1L)
    }

    verify(prisonVisitSlotRepository).findById(1L)
  }

  @Test
  fun `should create a visit slot and return it`() {
    val request = CreateVisitSlotRequest(dpsLocationId = UUID.randomUUID(), maxAdults = 10, maxGroups = 5, maxVideo = 2)
    val timeSlotEntity = prisonTimeSlotEntity()

    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlotEntity))
    whenever(prisonVisitSlotRepository.saveAndFlush(any<PrisonVisitSlotEntity>())).thenAnswer { it.arguments[0] }

    val created = service.create(1L, request, MOORLAND_PRISON_USER)

    val visitSlotCaptor = argumentCaptor<PrisonVisitSlotEntity>()
    verify(prisonVisitSlotRepository).saveAndFlush(visitSlotCaptor.capture())

    visitSlotCaptor.firstValue.assertWithResponse(created)
    created.assertWithCreateRequest(request)

    verify(prisonTimeSlotRepository).findById(1L)
    verify(prisonVisitSlotRepository).saveAndFlush(any<PrisonVisitSlotEntity>())
  }

  @Test
  fun `should fail to create a visit slot when time slot not found`() {
    val request = CreateVisitSlotRequest(dpsLocationId = UUID.randomUUID(), maxAdults = 10, maxGroups = 5, maxVideo = 2)
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      service.create(1L, request, MOORLAND_PRISON_USER)
    }

    verify(prisonTimeSlotRepository).findById(1L)
    verifyNoMoreInteractions(prisonVisitSlotRepository)
  }

  @Test
  fun `should update a visit slot capacities and return it`() {
    val request = UpdateVisitSlotRequest(maxAdults = 5, maxGroups = 3, maxVideo = 1, dpsLocationId = UUID.randomUUID())
    val existing = prisonVisitSlotEntity()
    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(existing))
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(prisonTimeSlotEntity()))
    whenever(prisonVisitSlotRepository.saveAndFlush(any<PrisonVisitSlotEntity>())).thenAnswer { it.arguments[0] }

    val updated = service.update(1L, request, MOORLAND_PRISON_USER)

    val visitSlotCaptor = argumentCaptor<PrisonVisitSlotEntity>()
    verify(prisonVisitSlotRepository).saveAndFlush(visitSlotCaptor.capture())

    visitSlotCaptor.firstValue.assertWithResponse(updated)
    verify(prisonVisitSlotRepository).findById(1L)
  }

  @Test
  fun `should fail to update visit slot which does not exist`() {
    val updateRequest = UpdateVisitSlotRequest(maxAdults = 1, maxGroups = 1, maxVideo = 0)
    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.empty())
    assertThrows<EntityNotFoundException> {
      service.update(1L, updateRequest, MOORLAND_PRISON_USER)
    }
    verify(prisonVisitSlotRepository).findById(1L)
    verifyNoMoreInteractions(prisonVisitSlotRepository)
  }

  @Test
  fun `should delete visit slot when there is no associated official visits exists`() {
    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(prisonVisitSlotEntity()))
    whenever(officialVisitRepository.existsByPrisonVisitSlotPrisonVisitSlotId(1L)).thenReturn(false)
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(prisonTimeSlotEntity()))

    service.delete(1L)
    verify(prisonVisitSlotRepository).deleteById(1L)

    verify(prisonVisitSlotRepository).findById(1L)
    verify(officialVisitRepository).existsByPrisonVisitSlotPrisonVisitSlotId(1L)
    verify(prisonTimeSlotRepository).findById(1L)
  }

  @Test
  fun `should fail to delete visit slot when there is associated official visit exists`() {
    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(prisonVisitSlotEntity()))
    whenever(officialVisitRepository.existsByPrisonVisitSlotPrisonVisitSlotId(1L)).thenReturn(true)

    val exception = assertThrows<EntityInUseException> {
      service.delete(1L)
    }

    val expectedException = EntityInUseException("The prison visit slot has visits associated with it and cannot be deleted.")
    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(prisonVisitSlotRepository).findById(1L)
    verify(officialVisitRepository).existsByPrisonVisitSlotPrisonVisitSlotId(1L)
    verifyNoMoreInteractions(prisonTimeSlotRepository)
  }

  private fun PrisonVisitSlotEntity.assertWithResponse(model: VisitSlot) {
    assertThat(prisonVisitSlotId).isEqualTo(model.visitSlotId)
    assertThat(prisonTimeSlotId).isEqualTo(model.prisonTimeSlotId)
    assertThat(dpsLocationId).isEqualTo(model.dpsLocationId)
    assertThat(maxAdults).isEqualTo(model.maxAdults)
    assertThat(maxGroups).isEqualTo(model.maxGroups)
    assertThat(maxVideoSessions).isEqualTo(model.maxVideo)
    assertThat(createdBy).isEqualTo(model.createdBy)
    assertThat(createdTime).isEqualTo(model.createdTime)
    assertThat(updatedTime).isEqualTo(model.updatedTime)
    assertThat(updatedBy).isEqualTo(model.updatedBy)
  }

  private fun prisonVisitSlotEntity() = PrisonVisitSlotEntity(
    prisonVisitSlotId = 1L,
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    maxGroups = 5,
    maxVideoSessions = 2,
    createdBy = "Test",
    createdTime = createdTime,
  )

  private fun prisonTimeSlotEntity() = PrisonTimeSlotEntity(
    prisonTimeSlotId = 1L,
    prisonCode = "MDI",
    dayCode = DayType.MON,
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    effectiveDate = LocalDate.now(),
    expiryDate = LocalDate.now().plusDays(365),
    createdBy = "Test",
    createdTime = createdTime,
  )

  private fun VisitSlot.assertWithCreateRequest(request: CreateVisitSlotRequest) {
    assertThat(prisonTimeSlotId).isEqualTo(1L)
    assertThat(dpsLocationId).isEqualTo(request.dpsLocationId)
    assertThat(maxAdults).isEqualTo(request.maxAdults)
    assertThat(maxGroups).isEqualTo(request.maxGroups)
    assertThat(maxVideo).isEqualTo(request.maxVideo)
  }
}
