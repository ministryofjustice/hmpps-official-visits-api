package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlotSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlotSummaryItem
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class ReconciliationServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()

  private val reconciliationService = ReconciliationService(officialVisitRepository, prisonerVisitedRepository, prisonTimeSlotRepository, prisonVisitSlotRepository)

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `Get all official visits Ids`() {
    val pageable = PageRequest.of(0, 10)

    val result = listOf<Long>(1L)
    val pageOfficialVisitsIds = PageImpl(result, pageable, 1)

    whenever(officialVisitRepository.findAllOfficialVisitIds(null, pageable)).thenReturn(pageOfficialVisitsIds)

    assertThat(
      reconciliationService.getOfficialVisitIds(false, pageable).content.single().officialVisitId isEqualTo 1,
    )
  }

  @Test
  fun `should return summary of active time slots and associated visit slots for the prison code`() {
    val request = prisonVisitSlotEntity(1L)
    val timeSlotEntity = prisonTimeSlotEntity(1L)
    whenever(prisonTimeSlotRepository.findAllActiveByPrisonCode("MDI")).thenReturn(listOf(timeSlotEntity))
    whenever(prisonVisitSlotRepository.findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))).thenReturn(listOf(request))
    val summary = reconciliationService.getAllPrisonTimeSlotsAndAssociatedVisitSlot("MDI", true)
    assertThat(summary).isEqualTo(SyncTimeSlotSummary(prisonCode = "MDI", listOf(SyncTimeSlotSummaryItem(timeSlotEntity.toSyncModel(), listOf(request.toSyncModel("MDI"))))))
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
    val summary = reconciliationService.getAllPrisonTimeSlotsAndAssociatedVisitSlot("MDI", false)
    assertThat(summary).isEqualTo(SyncTimeSlotSummary(prisonCode = "MDI", listOf(SyncTimeSlotSummaryItem(timeSlotEntity.toSyncModel(), listOf(request.toSyncModel("MDI"))))))
    verify(prisonTimeSlotRepository).findAllByPrisonCode("MDI")
    verify(prisonVisitSlotRepository).findByPrisonTimeSlotIdIn(listOf(timeSlotEntity.prisonTimeSlotId))
    verifyNoMoreInteractions(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  private fun prisonVisitSlotEntity(prisonVisitSlotId: Long = 1L) = PrisonVisitSlotEntity(
    prisonVisitSlotId = prisonVisitSlotId,
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    createdBy = "Test",
    createdTime = createdTime,
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
