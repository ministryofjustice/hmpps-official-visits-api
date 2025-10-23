package uk.gov.justice.digital.hmpps.officialvisitsapi.service.migrate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitConfigRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.IdPair
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class MigrateVisitServiceTest {
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()

  val migrationService = MigrationService(prisonTimeSlotRepository, prisonVisitSlotRepository)

  private val aUsername = "TEST"
  private val aDateTime = LocalDateTime.of(2024, 1, 1, 13, 0)
  private val slotStart = LocalTime.of(10, 0)
  private val slotEnd = LocalTime.of(11, 0)

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @AfterEach
  fun resetMocks() {
    reset(prisonTimeSlotRepository, prisonVisitSlotRepository)
  }

  @Nested
  inner class VisitConfiguration {
    @Test
    fun `should migrate one time slot with two visit slots`() {
      val request = migrateVisitConfigRequest(prisonCode = "MDI", dayCode = "TUE", timeSlotSeq = 1)

      val timeSlotEntity = PrisonTimeSlotEntity(
        prisonTimeSlotId = 1L,
        prisonCode = "MDI",
        dayCode = "TUE",
        startTime = slotStart,
        endTime = slotEnd,
        effectiveDate = aDateTime.toLocalDate(),
        createdTime = aDateTime,
        createdBy = aUsername,
      )

      val visitSlotEntities = visitSlotEntities(request)

      whenever(prisonTimeSlotRepository.saveAndFlush(any())).thenReturn(timeSlotEntity)

      whenever(prisonVisitSlotRepository.saveAndFlush(any()))
        .thenReturn(visitSlotEntities[0])
        .thenReturn(visitSlotEntities[1])

      val timeSlotCaptor = argumentCaptor<PrisonTimeSlotEntity>()
      val visitSlotCaptor = argumentCaptor<PrisonVisitSlotEntity>()

      val result = migrationService.migrateVisitConfiguration(request)

      // Reflecting key values from the request back in the response
      assertThat(result.prisonCode).isEqualTo(request.prisonCode)
      assertThat(result.dayCode).isEqualTo(request.dayCode)
      assertThat(result.timeSlotSeq).isEqualTo(request.timeSlotSeq)

      // Providing the slot ID mappings
      assertThat(result.visitSlots).containsAll(
        listOf(
          IdPair(elementType = ElementType.PRISON_VISIT_SLOT, 1L, 1L),
          IdPair(elementType = ElementType.PRISON_VISIT_SLOT, 2L, 2L),
        ),
      )

      verify(prisonTimeSlotRepository).saveAndFlush(timeSlotCaptor.capture())

      with(timeSlotCaptor.firstValue) {
        assertThat(this)
          .extracting("prisonCode", "dayCode", "startTime", "endTime", "effectiveDate", "expiryDate", "createdBy", "createdTime")
          .contains(
            request.prisonCode,
            request.dayCode,
            request.startTime,
            request.endTime,
            request.effectiveDate,
            request.expiryDate,
            request.createUsername,
            request.createDateTime,
          )
      }

      verify(prisonVisitSlotRepository, times(2)).saveAndFlush(visitSlotCaptor.capture())

      for (x in 0..1) {
        with(visitSlotCaptor.allValues[x]) {
          assertThat(this)
            .extracting("prisonTimeSlotId", "dpsLocationId", "maxAdults", "maxGroups", "maxVideoSessions", "createdBy", "createdTime")
            .contains(
              visitSlotEntities[x].prisonTimeSlotId,
              visitSlotEntities[x].dpsLocationId,
              visitSlotEntities[x].maxAdults,
              visitSlotEntities[x].maxGroups,
              visitSlotEntities[x].maxVideoSessions,
              visitSlotEntities[x].createdBy,
              visitSlotEntities[x].createdTime,
            )
        }
      }
    }

    @Test
    fun `should extract and save two visit slots from the request`() {
      val request = migrateVisitConfigRequest(prisonCode = "LEI", dayCode = "MON", timeSlotSeq = 2)

      val visitSlotEntities = visitSlotEntities(request)

      whenever(prisonVisitSlotRepository.saveAndFlush(any()))
        .thenReturn(visitSlotEntities[0])
        .thenReturn(visitSlotEntities[1])

      val visitSlotCaptor = argumentCaptor<PrisonVisitSlotEntity>()

      val result = migrationService.extractAndSaveVisitSlots(1L, request)

      assertThat(result.size).isEqualTo(2)

      for (i in 0..1) {
        assertThat(result[i].first).isEqualTo(request.visitSlots[i].agencyVisitSlotId)
        assertThat(result[i].second)
          .extracting("prisonTimeSlotId", "dpsLocationId", "maxAdults", "maxGroups", "maxVideoSessions", "createdBy", "createdTime")
          .contains(
            visitSlotEntities[i].prisonTimeSlotId,
            visitSlotEntities[i].dpsLocationId,
            visitSlotEntities[i].maxAdults,
            visitSlotEntities[i].maxGroups,
            visitSlotEntities[i].maxVideoSessions,
            visitSlotEntities[i].createdBy,
            visitSlotEntities[i].createdTime,
          )
      }

      verify(prisonVisitSlotRepository, times(2)).saveAndFlush(visitSlotCaptor.capture())

      for (i in 0..1) {
        with(visitSlotCaptor.allValues[i]) {
          assertThat(this)
            .extracting("prisonTimeSlotId", "dpsLocationId", "maxAdults", "maxGroups", "maxVideoSessions", "createdBy", "createdTime")
            .contains(
              visitSlotEntities[i].prisonTimeSlotId,
              visitSlotEntities[i].dpsLocationId,
              visitSlotEntities[i].maxAdults,
              visitSlotEntities[i].maxGroups,
              visitSlotEntities[i].maxVideoSessions,
              visitSlotEntities[i].createdBy,
              visitSlotEntities[i].createdTime,
            )
        }
      }
    }
  }

  private fun migrateVisitConfigRequest(prisonCode: String, dayCode: String, timeSlotSeq: Int) = MigrateVisitConfigRequest(
    prisonCode = prisonCode,
    dayCode = dayCode,
    timeSlotSeq = timeSlotSeq,
    startTime = slotStart,
    endTime = slotEnd,
    effectiveDate = aDateTime.toLocalDate(),
    createDateTime = aDateTime,
    createUsername = aUsername,
    visitSlots = listOf(
      MigrateVisitSlot(
        agencyVisitSlotId = 1L,
        dpsLocationId = UUID.fromString("aa0df03b-7864-47d5-9729-0301b74ecbe2"),
        maxGroups = 8,
        maxAdults = 16,
        maxVideoSessions = 16,
        createDateTime = aDateTime,
        createUsername = aUsername,
      ),
      MigrateVisitSlot(
        agencyVisitSlotId = 2L,
        dpsLocationId = UUID.fromString("bb0df03b-7864-47d5-9729-0301b74ecbe2"),
        maxGroups = 4,
        maxAdults = 8,
        maxVideoSessions = 8,
        createDateTime = aDateTime,
        createUsername = aUsername,
      ),
    ),
  )

  private fun visitSlotEntities(request: MigrateVisitConfigRequest) = listOf(
    PrisonVisitSlotEntity(
      prisonVisitSlotId = 1L,
      prisonTimeSlotId = 1L,
      dpsLocationId = request.visitSlots[0].dpsLocationId!!,
      maxAdults = request.visitSlots[0].maxAdults!!,
      maxGroups = request.visitSlots[0].maxGroups!!,
      maxVideoSessions = request.visitSlots[0].maxVideoSessions!!,
      createdBy = aUsername,
      createdTime = aDateTime,
    ),
    PrisonVisitSlotEntity(
      prisonVisitSlotId = 2L,
      prisonTimeSlotId = 1L,
      dpsLocationId = request.visitSlots[1].dpsLocationId!!,
      maxAdults = request.visitSlots[1].maxAdults!!,
      maxGroups = request.visitSlots[1].maxGroups!!,
      maxVideoSessions = request.visitSlots[1].maxVideoSessions!!,
      createdBy = aUsername,
      createdTime = aDateTime,
    ),
  )
}
