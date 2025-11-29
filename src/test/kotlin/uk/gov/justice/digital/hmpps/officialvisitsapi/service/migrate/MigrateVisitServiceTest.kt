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
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.CodedValue
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitConfigRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.IdPair
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class MigrateVisitServiceTest {
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val officialVisitorRepository: OfficialVisitorRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()

  val migrationService = MigrationService(
    prisonTimeSlotRepository,
    prisonVisitSlotRepository,
    officialVisitRepository,
    officialVisitorRepository,
    prisonerVisitedRepository,
  )

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
          maxGroups = null,
          maxAdults = null,
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
        maxAdults = request.visitSlots[0].maxAdults,
        maxGroups = request.visitSlots[0].maxGroups,
        maxVideoSessions = request.visitSlots[0].maxVideoSessions,
        createdBy = aUsername,
        createdTime = aDateTime,
      ),
      PrisonVisitSlotEntity(
        prisonVisitSlotId = 2L,
        prisonTimeSlotId = 1L,
        dpsLocationId = request.visitSlots[1].dpsLocationId!!,
        maxAdults = request.visitSlots[1].maxAdults,
        maxGroups = request.visitSlots[1].maxGroups,
        maxVideoSessions = request.visitSlots[1].maxVideoSessions,
        createdBy = aUsername,
        createdTime = aDateTime,
      ),
    )
  }

  @Nested
  inner class Visit {
    @Test
    fun `should migrate one visit with two visitors`() {
      val request = migrateVisitRequest(prisonCode = "MDI", prisonVisitSlotId = 1L)

      // Create the stubbed values
      val visitSlotEntity = PrisonVisitSlotEntity(
        prisonVisitSlotId = 1L,
        prisonTimeSlotId = 1L,
        dpsLocationId = UUID.randomUUID(),
        maxAdults = 8,
        maxGroups = 4,
        maxVideoSessions = 2,
        createdTime = aDateTime,
        createdBy = aUsername,
      )
      val visitEntity = visitEntity(request, visitSlotEntity)
      val prisonerVisitedEntity = prisonerVisitedEntity(request, visitEntity)
      val visitorEntities = visitorEntities(request, visitEntity)

      // Stub the values returned
      whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(Optional.of(visitSlotEntity))
      whenever(officialVisitRepository.saveAndFlush(any())).thenReturn(visitEntity)
      whenever(prisonerVisitedRepository.saveAndFlush(any())).thenReturn(prisonerVisitedEntity)
      whenever(officialVisitorRepository.saveAndFlush(any()))
        .thenReturn(visitorEntities[0])
        .thenReturn(visitorEntities[1])

      // Capture the values saved for visits and visitors
      val visitCaptor = argumentCaptor<OfficialVisitEntity>()
      val visitorCaptor = argumentCaptor<OfficialVisitorEntity>()

      val result = migrationService.migrateVisit(request)

      // Check responses generated
      assertThat(result.visit).isEqualTo(IdPair(elementType = ElementType.OFFICIAL_VISIT, 1L, 1L))
      assertThat(result.prisoner).isEqualTo(IdPair(elementType = ElementType.PRISONER_VISITED, 222, 1L))
      assertThat(result.visitors).containsAll(
        listOf(
          IdPair(elementType = ElementType.OFFICIAL_VISITOR, 1L, 1L),
          IdPair(elementType = ElementType.OFFICIAL_VISITOR, 2L, 2L),
        ),
      )

      // Get the captured values saved
      verify(officialVisitRepository).saveAndFlush(visitCaptor.capture())
      verify(officialVisitorRepository, times(2)).saveAndFlush(visitorCaptor.capture())

      // Check the visit details saved
      with(visitCaptor.firstValue) {
        assertThat(this)
          .extracting("prisonCode", "prisonerNumber", "visitDate", "startTime", "endTime", "dpsLocationId", "createdBy", "createdTime", "offenderVisitId")
          .contains(
            request.prisonCode,
            request.prisonerNumber,
            request.visitDate,
            request.startTime,
            request.endTime,
            request.dpsLocationId,
            request.createUsername,
            request.createDateTime,
            request.offenderVisitId,
          )
      }

      // Check the visitors details saved
      for (i in 0..1) {
        with(visitorCaptor.allValues[i]) {
          assertThat(this)
            .extracting("contactId", "prisonerContactId", "firstName", "lastName", "leadVisitor", "assistedVisit", "createdBy", "createdTime")
            .contains(
              visitorEntities[i].contactId,
              visitorEntities[i].prisonerContactId,
              visitorEntities[i].firstName,
              visitorEntities[i].lastName,
              visitorEntities[i].leadVisitor,
              visitorEntities[i].assistedVisit,
              visitorEntities[i].createdBy,
              visitorEntities[i].createdTime,
            )
        }
      }
    }

    private fun migrateVisitRequest(prisonCode: String, prisonVisitSlotId: Long) = MigrateVisitRequest(
      offenderVisitId = 1,
      prisonVisitSlotId = prisonVisitSlotId,
      prisonCode = prisonCode,
      offenderBookId = 222,
      prisonerNumber = "A1234AA",
      visitDate = today().plusDays(1),
      startTime = slotStart,
      endTime = slotEnd,
      dpsLocationId = UUID.randomUUID(),
      currentTerm = true,
      visitStatusCode = CodedValue("SCHEDULED", "Scheduled"),
      commentText = "A comment",
      visitorConcernText = "Concern text",
      searchTypeCode = CodedValue("RUB_A", "Pat down search"),
      eventOutcomeCode = CodedValue("NORMAL", "Normal"),
      outcomeReasonCode = CodedValue("COMPLETE", "Completed"),
      overrideBanStaffUsername = "XXX",
      createDateTime = aDateTime,
      createUsername = aUsername,
      visitors = listOf(
        MigrateVisitor(
          offenderVisitVisitorId = 1,
          personId = 11111,
          firstName = "Mary",
          lastName = "Smith",
          dateOfBirth = LocalDate.of(2001, 1, 1),
          relationshipToPrisoner = CodedValue("SIS", "Sister"),
          groupLeaderFlag = true,
          assistedVisitFlag = false,
          commentText = "comment1",
          createDateTime = aDateTime,
          createUsername = aUsername,
        ),
        MigrateVisitor(
          offenderVisitVisitorId = 2,
          personId = 22222,
          firstName = "John",
          lastName = "Smith",
          dateOfBirth = LocalDate.of(2001, 2, 2),
          relationshipToPrisoner = CodedValue("BRO", "Brother"),
          groupLeaderFlag = false,
          assistedVisitFlag = true,
          commentText = "comment2",
          createDateTime = aDateTime,
          createUsername = aUsername,
        ),
      ),
    )

    private fun visitEntity(request: MigrateVisitRequest, visitSlot: PrisonVisitSlotEntity) = OfficialVisitEntity(
      officialVisitId = 1L,
      prisonVisitSlot = visitSlot,
      prisonCode = request.prisonCode!!,
      prisonerNumber = request.prisonerNumber!!,
      visitStatusCode = "SCHEDULED",
      visitTypeCode = "IN_PERSON",
      visitDate = request.visitDate!!,
      startTime = request.startTime!!,
      endTime = request.endTime!!,
      dpsLocationId = request.dpsLocationId!!,
      currentTerm = request.currentTerm!!,
      createdBy = aUsername,
      offenderVisitId = request.offenderVisitId!!,
    )

    private fun prisonerVisitedEntity(request: MigrateVisitRequest, visit: OfficialVisitEntity) = PrisonerVisitedEntity(
      prisonerVisitedId = 1L,
      officialVisit = visit,
      prisonerNumber = request.prisonerNumber!!,
      createdBy = aUsername,
      createdTime = aDateTime,
    )

    private fun visitorEntities(request: MigrateVisitRequest, visit: OfficialVisitEntity) = listOf(
      OfficialVisitorEntity(
        officialVisitorId = 1L,
        officialVisit = visit,
        visitorTypeCode = "CONTACT",
        contactId = request.visitors[0].personId,
        firstName = request.visitors[0].firstName,
        lastName = request.visitors[0].lastName,
        relationshipCode = request.visitors[0].relationshipToPrisoner?.code,
        prisonerContactId = null,
        contactTypeCode = "OFFICIAL",
        leadVisitor = true,
        assistedVisit = false,
        createdBy = aUsername,
        createdTime = aDateTime,
        offenderVisitVisitorId = request.visitors[0].offenderVisitVisitorId!!,
      ),
      OfficialVisitorEntity(
        officialVisitorId = 2L,
        officialVisit = visit,
        visitorTypeCode = "CONTACT",
        contactId = request.visitors[1].personId,
        firstName = request.visitors[1].firstName,
        lastName = request.visitors[1].lastName,
        relationshipCode = request.visitors[1].relationshipToPrisoner?.code,
        prisonerContactId = null,
        contactTypeCode = "SOCIAL",
        leadVisitor = false,
        assistedVisit = true,
        createdBy = aUsername,
        createdTime = aDateTime,
        offenderVisitVisitorId = request.visitors[1].offenderVisitVisitorId!!,
      ),
    )
  }
}
