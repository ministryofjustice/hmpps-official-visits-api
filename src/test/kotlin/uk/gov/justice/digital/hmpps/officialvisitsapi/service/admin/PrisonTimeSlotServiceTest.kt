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
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.NonResidentialUsageDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toTimeSlotModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toVisitSlotModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummaryItem
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class PrisonTimeSlotServiceTest {
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val locationService: LocationsService = mock()
  private val timeSlotService = PrisonTimeSlotService(prisonTimeSlotRepository, prisonVisitSlotRepository, prisonerSearchClient, locationService)

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  private val dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")
  private val prisonTimeSlotService =
    PrisonTimeSlotService(prisonTimeSlotRepository, prisonVisitSlotRepository, prisonerSearchClient, locationService)

  @AfterEach
  fun afterEach() {
    reset(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `should get a time slot by ID`() {
    val timeSlotEntity = prisonTimeSlotEntity()
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

    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(prisonTimeSlotEntity()))
    whenever(prisonTimeSlotRepository.saveAndFlush(any())).thenReturn(prisonTimeSlotEntity())

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
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(prisonTimeSlotEntity()))
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
    whenever(prisonTimeSlotRepository.findById(1L)).thenReturn(Optional.of(prisonTimeSlotEntity()))
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

  @Test
  fun `should return summary of active time slots and associated visit slots for the prison code`() {
    val timeSlotEntity = PrisonTimeSlotEntity(
      prisonTimeSlotId = 1L,
      prisonCode = MOORLAND_PRISONER.prison,
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusDays(365),
      createdBy = "Test",
      createdTime = LocalDateTime.now(),
    )

    val visitSlotEntity = PrisonVisitSlotEntity(
      prisonVisitSlotId = 1L,
      prisonTimeSlotId = 1L,
      dpsLocationId = dpsLocationId,
      maxAdults = 10,
      createdBy = "Test",
      createdTime = LocalDateTime.now(),
    )

    whenever(prisonTimeSlotRepository.findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)).thenReturn(
      listOf(
        timeSlotEntity,
      ),
    )
    whenever(prisonVisitSlotRepository.findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))).thenReturn(
      listOf(visitSlotEntity),
    )
    whenever(prisonerSearchClient.findPrisonName(MOORLAND_PRISONER.prison)).thenReturn("A prison")

    whenever(locationService.getOfficialVisitLocationsAtPrison(MOORLAND_PRISONER.prison))
      .thenReturn(officialVisitLocations(dpsLocationId))

    val summary = prisonTimeSlotService.getAllPrisonTimeSlotsAndAssociatedVisitSlots(MOORLAND_PRISONER.prison, true)

    assertThat(summary).isEqualTo(
      TimeSlotSummary(
        prisonCode = MOORLAND_PRISONER.prison,
        prisonName = "A prison",
        timeSlots =
        listOf(
          TimeSlotSummaryItem(
            timeSlotEntity.toTimeSlotModel(),
            listOf(
              visitSlotEntity.toVisitSlotModel(MOORLAND_PRISONER.prison).copy(
                locationDescription = location(dpsLocationId).localName,
                locationMaxCapacity = 10,
                locationType = location(dpsLocationId).locationType.value,
              ),
            ),
          ),
        ),
      ),
    )

    verify(prisonTimeSlotRepository).findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)
    verify(prisonVisitSlotRepository).findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))
    verifyNoMoreInteractions(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `should return summary with no time slots when there are none active for the prison code`() {
    whenever(prisonTimeSlotRepository.findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)).thenReturn(emptyList())

    whenever(prisonerSearchClient.findPrisonName(MOORLAND_PRISONER.prison)).thenReturn("A prison")
    whenever(locationService.getOfficialVisitLocationsAtPrison(MOORLAND_PRISONER.prison)).thenReturn(emptyList())

    val summary = prisonTimeSlotService.getAllPrisonTimeSlotsAndAssociatedVisitSlots(MOORLAND_PRISONER.prison, true)

    assertThat(summary).isEqualTo(
      TimeSlotSummary(
        prisonCode = MOORLAND_PRISONER.prison,
        prisonName = "A prison",
        timeSlots = emptyList(),
      ),
    )

    verify(prisonTimeSlotRepository).findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)
    verifyNoMoreInteractions(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `should return summary with time slots and no visit slots when there are no matching visit slots`() {
    val timeSlotEntity = PrisonTimeSlotEntity(
      prisonTimeSlotId = 1L,
      prisonCode = MOORLAND_PRISONER.prison,
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusDays(365),
      createdBy = "Test",
      createdTime = LocalDateTime.now(),
    )

    whenever(prisonTimeSlotRepository.findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)).thenReturn(
      listOf(
        timeSlotEntity,
      ),
    )
    whenever(prisonVisitSlotRepository.findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))).thenReturn(
      emptyList(),
    )
    whenever(prisonerSearchClient.findPrisonName(MOORLAND_PRISONER.prison)).thenReturn("A prison")

    whenever(locationService.getOfficialVisitLocationsAtPrison(MOORLAND_PRISONER.prison))
      .thenReturn(officialVisitLocations(dpsLocationId))

    val summary = prisonTimeSlotService.getAllPrisonTimeSlotsAndAssociatedVisitSlots(MOORLAND_PRISONER.prison, true)

    assertThat(summary).isEqualTo(
      TimeSlotSummary(
        prisonCode = MOORLAND_PRISONER.prison,
        prisonName = "A prison",
        timeSlots =
        listOf(
          TimeSlotSummaryItem(
            timeSlotEntity.toTimeSlotModel(),
            emptyList(),
          ),
        ),
      ),
    )

    verify(prisonTimeSlotRepository).findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)
    verify(prisonVisitSlotRepository).findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))
    verifyNoMoreInteractions(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `should return summary with time slots and inactive visit slots when activeOnly is false`() {
    val timeSlotEntity = PrisonTimeSlotEntity(
      prisonTimeSlotId = 1L,
      prisonCode = MOORLAND_PRISONER.prison,
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusDays(365),
      createdBy = "Test",
      createdTime = LocalDateTime.now(),
    )

    val visitSlotEntity = PrisonVisitSlotEntity(
      prisonVisitSlotId = 1L,
      prisonTimeSlotId = 1L,
      dpsLocationId = dpsLocationId,
      maxAdults = 10,
      createdBy = "Test",
      createdTime = LocalDateTime.now(),
    )

    val inactiveVisitSlotEntity = visitSlotEntity.copy(
      prisonVisitSlotId = 2L,
      maxAdults = 0,
    )

    whenever(prisonTimeSlotRepository.findAllByPrisonCode(MOORLAND_PRISONER.prison)).thenReturn(
      listOf(
        timeSlotEntity,
      ),
    )
    whenever(prisonVisitSlotRepository.findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))).thenReturn(
      listOf(visitSlotEntity, inactiveVisitSlotEntity),
    )
    whenever(prisonerSearchClient.findPrisonName(MOORLAND_PRISONER.prison)).thenReturn("A prison")

    whenever(locationService.getOfficialVisitLocationsAtPrison(MOORLAND_PRISONER.prison))
      .thenReturn(officialVisitLocations(dpsLocationId))

    val summary = prisonTimeSlotService.getAllPrisonTimeSlotsAndAssociatedVisitSlots(MOORLAND_PRISONER.prison, false)

    assertThat(summary).isEqualTo(
      TimeSlotSummary(
        prisonCode = MOORLAND_PRISONER.prison,
        prisonName = "A prison",
        timeSlots =
        listOf(
          TimeSlotSummaryItem(
            timeSlotEntity.toTimeSlotModel(),
            listOf(
              visitSlotEntity.toVisitSlotModel(MOORLAND_PRISONER.prison).copy(
                locationDescription = location(dpsLocationId).localName,
                locationMaxCapacity = 10,
                locationType = location(dpsLocationId).locationType.value,
              ),
              inactiveVisitSlotEntity.toVisitSlotModel(MOORLAND_PRISONER.prison).copy(
                locationDescription = location(dpsLocationId).localName,
                locationMaxCapacity = 10,
                locationType = location(dpsLocationId).locationType.value,
              ),
            ),
          ),
        ),
      ),
    )

    verify(prisonTimeSlotRepository).findAllByPrisonCode(MOORLAND_PRISONER.prison)
    verify(prisonVisitSlotRepository).findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))
    verifyNoMoreInteractions(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  @Test
  fun `should mark visit slot location as unknown when location missing from official list`() {
    val timeSlotEntity = PrisonTimeSlotEntity(
      prisonTimeSlotId = 1L,
      prisonCode = MOORLAND_PRISONER.prison,
      dayCode = DayType.MON,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      effectiveDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusDays(365),
      createdBy = "Test",
      createdTime = LocalDateTime.now(),
    )

    val visitSlotEntity = PrisonVisitSlotEntity(
      prisonVisitSlotId = 1L,
      prisonTimeSlotId = 1L,
      dpsLocationId = dpsLocationId,
      maxAdults = 10,
      createdBy = "Test",
      createdTime = LocalDateTime.now(),
    )

    whenever(prisonTimeSlotRepository.findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)).thenReturn(listOf(timeSlotEntity))
    whenever(prisonVisitSlotRepository.findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))).thenReturn(listOf(visitSlotEntity))
    whenever(prisonerSearchClient.findPrisonName(MOORLAND_PRISONER.prison)).thenReturn("A prison")

    // official locations do not include the dpsLocationId used by the visit slot
    whenever(locationService.getOfficialVisitLocationsAtPrison(MOORLAND_PRISONER.prison)).thenReturn(emptyList())

    val summary = prisonTimeSlotService.getAllPrisonTimeSlotsAndAssociatedVisitSlots(MOORLAND_PRISONER.prison, true)

    assertThat(summary).isEqualTo(
      TimeSlotSummary(
        prisonCode = MOORLAND_PRISONER.prison,
        prisonName = "A prison",
        timeSlots = listOf(
          TimeSlotSummaryItem(
            timeSlotEntity.toTimeSlotModel(),
            listOf(
              visitSlotEntity.toVisitSlotModel(MOORLAND_PRISONER.prison).copy(
                locationDescription = "** unknown **",
              ),
            ),
          ),
        ),
      ),
    )

    verify(prisonTimeSlotRepository).findAllActiveByPrisonCode(MOORLAND_PRISONER.prison)
    verify(prisonVisitSlotRepository).findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))
    verifyNoMoreInteractions(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  private fun officialVisitLocations(dpsLocationId: UUID) = listOf(
    location(dpsLocationId),
  )

  private fun location(dpsLocationId: UUID): Location = Location(
    id = dpsLocationId,
    prisonId = MOORLAND,
    localName = "A name",
    code = "Code",
    pathHierarchy = "A-1-1-1",
    locationType = Location.LocationType.VISITS,
    usage = listOf(NonResidentialUsageDto(usageType = NonResidentialUsageDto.UsageType.VISIT, sequence = 99, capacity = 10)),
    permanentlyInactive = false,
    status = Location.Status.ACTIVE,
    level = 3,
    key = "A-1-1-1",
    active = true,
    locked = false,
    isResidential = false,
    leafLevel = true,
    topLevelId = UUID.randomUUID(),
    deactivatedByParent = false,
    lastModifiedBy = "XXX",
    lastModifiedDate = LocalDateTime.now().minusDays(1),
  )

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

  private fun prisonTimeSlotEntity() = PrisonTimeSlotEntity(
    prisonTimeSlotId = 1L,
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
}
