package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.xml.bind.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitorEquipmentEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation2
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toPrisonerContactModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateCommentRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditEventDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class OfficialVisitUpdateServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val officialVisitorRepository: OfficialVisitorRepository = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val contactsService: ContactsService = mock()
  private val metricsService: MetricsService = mock()
  private val locationsService: LocationsService = mock()
  private val auditingService: AuditingService = mock()

  private val service = OfficialVisitUpdateService(
    officialVisitRepository,
    officialVisitorRepository,
    prisonVisitSlotRepository,
    contactsService,
    metricsService,
    locationsService,
    auditingService,
  )

  @AfterEach
  fun afterEach() {
    reset(officialVisitRepository, officialVisitorRepository, prisonVisitSlotRepository, contactsService)
  }

  @Test
  fun `updateVisitTypeAndSlot updates visit and returns response`() {
    val visit = createVisit(officialVisitId = 11L)
    val newSlot = createSlot(99L)
    val request = OfficialVisitUpdateSlotRequest(
      prisonVisitSlotId = 99L,
      visitDate = LocalDate.now().plusDays(2),
      startTime = LocalTime.of(14, 0),
      endTime = LocalTime.of(15, 0),
      dpsLocationId = UUID.randomUUID(),
      visitTypeCode = VisitType.VIDEO,
    )

    whenever { locationsService.getLocationById(visit.dpsLocationId) } doReturn moorlandLocation
    whenever { locationsService.getLocationById(request.dpsLocationId) } doReturn moorlandLocation2
    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(11L, MOORLAND)).thenReturn(visit)
    whenever(prisonVisitSlotRepository.findById(99L)).thenReturn(Optional.of(newSlot))
    whenever(officialVisitRepository.saveAndFlush(any<OfficialVisitEntity>())).thenAnswer { it.arguments[0] as OfficialVisitEntity }

    val oldVisitTypeCode = visit.visitTypeCode
    val oldVisitDate = visit.visitDate
    val oldStartTime = visit.startTime
    val oldEndTime = visit.endTime

    val response = service.updateVisitTypeAndSlot(11L, MOORLAND, request, MOORLAND_PRISON_USER)

    assertThat(response.officialVisitId).isEqualTo(11L)
    assertThat(response.prisonerNumber).isEqualTo(MOORLAND_PRISONER.number)
    assertThat(visit.prisonVisitSlot).isEqualTo(newSlot)
    assertThat(visit.visitDate).isEqualTo(request.visitDate)
    assertThat(visit.startTime).isEqualTo(request.startTime)
    assertThat(visit.endTime).isEqualTo(request.endTime)
    assertThat(visit.dpsLocationId).isEqualTo(request.dpsLocationId)
    assertThat(visit.visitTypeCode).isEqualTo(VisitType.VIDEO)
    assertThat(visit.updatedBy).isEqualTo(MOORLAND_PRISON_USER.username)
    assertThat(visit.updatedTime).isNotNull
    verify(officialVisitRepository).saveAndFlush(visit)

    val auditEventCaptor = argumentCaptor<AuditEventDto>()
    verify(auditingService).recordAuditEvent(auditEventCaptor.capture())

    with(auditEventCaptor.firstValue) {
      officialVisitId isEqualTo 11
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      prisonCode isEqualTo MOORLAND
      eventSource isEqualTo "DPS"
      username isEqualTo MOORLAND_PRISON_USER.username
      userFullName isEqualTo MOORLAND_PRISON_USER.name
      summaryText isEqualTo "Visit updated"
      detailText isEqualTo "visit_date|${oldVisitDate.toMediumFormatStyle()}|${request.visitDate.toMediumFormatStyle()};start_time|$oldStartTime|${request.startTime};end_time|$oldEndTime|${request.endTime};location|${moorlandLocation.localName}|${moorlandLocation2.localName};visit_type|$oldVisitTypeCode|VIDEO;visit_slot|1|99;"
      eventDateTime isCloseTo now()
    }
  }

  @Test
  fun `updateVisitTypeAndSlot throws not found exception error when visit not found`() {
    val request = OfficialVisitUpdateSlotRequest(
      prisonVisitSlotId = 1L,
      visitDate = LocalDate.now().plusDays(1),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationId = UUID.randomUUID(),
      visitTypeCode = VisitType.IN_PERSON,
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(1L, MOORLAND)).thenReturn(null)

    assertThrows<EntityNotFoundException> {
      service.updateVisitTypeAndSlot(1L, MOORLAND, request, MOORLAND_PRISON_USER)
    }
    verifyNoInteractions(prisonVisitSlotRepository)
    verify(officialVisitRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `updateVisitTypeAndSlot throws validation error when prison visit slot not found`() {
    val visit = createVisit(officialVisitId = 1L)
    val request = OfficialVisitUpdateSlotRequest(
      prisonVisitSlotId = 55L,
      visitDate = LocalDate.now().plusDays(1),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(10, 0),
      dpsLocationId = UUID.randomUUID(),
      visitTypeCode = VisitType.IN_PERSON,
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(1L, MOORLAND)).thenReturn(visit)
    whenever(prisonVisitSlotRepository.findById(55L)).thenReturn(Optional.empty())

    assertThrows<ValidationException> {
      service.updateVisitTypeAndSlot(1L, MOORLAND, request, MOORLAND_PRISON_USER)
    }
    verify(officialVisitRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `updateComments updates notes and returns response`() {
    val visit = createVisit(officialVisitId = 7L)
    val request = OfficialVisitUpdateCommentRequest(staffNotes = "staff updated", prisonerNotes = "prisoner updated")

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(7L, MOORLAND)).thenReturn(visit)
    whenever(officialVisitRepository.saveAndFlush(any<OfficialVisitEntity>())).thenAnswer { it.arguments[0] as OfficialVisitEntity }

    val response = service.updateComments(7L, MOORLAND, request, MOORLAND_PRISON_USER)

    assertThat(response.officialVisitId).isEqualTo(7L)
    assertThat(response.prisonerNumber).isEqualTo(MOORLAND_PRISONER.number)
    assertThat(visit.staffNotes).isEqualTo("staff updated")
    assertThat(visit.prisonerNotes).isEqualTo("prisoner updated")
    assertThat(visit.updatedBy).isEqualTo(MOORLAND_PRISON_USER.username)
    assertThat(visit.updatedTime).isNotNull
    verify(officialVisitRepository, times(1)).saveAndFlush(visit)

    val auditEventCaptor = argumentCaptor<AuditEventDto>()
    verify(auditingService).recordAuditEvent(auditEventCaptor.capture())

    with(auditEventCaptor.firstValue) {
      officialVisitId isEqualTo 7
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      prisonCode isEqualTo MOORLAND
      eventSource isEqualTo "DPS"
      username isEqualTo MOORLAND_PRISON_USER.username
      userFullName isEqualTo MOORLAND_PRISON_USER.name
      summaryText isEqualTo "Visit updated"
      detailText isEqualTo "prisoner_notes||prisoner updated;staff_notes||staff updated;"
      eventDateTime isCloseTo now()
    }
  }

  @Test
  fun `updateComments throws not found error when visit not found`() {
    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(9L, MOORLAND)).thenReturn(null)

    assertThrows<EntityNotFoundException> {
      service.updateComments(9L, MOORLAND, OfficialVisitUpdateCommentRequest("a", "b"), MOORLAND_PRISON_USER)
    }
    verify(officialVisitRepository, never()).saveAndFlush(any())
  }

  // ---------------------------------------------------------------------------------------------------------------
  // updateVisitors - validation (all must fail before anything is changed)
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `updateVisitors throws not found error when visit not found`() {
    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(99L, MOORLAND)).thenReturn(null)

    assertThrows<EntityNotFoundException> {
      service.updateVisitors(99L, MOORLAND, OfficialVisitUpdateVisitorsRequest(emptyList()), MOORLAND_PRISON_USER)
    }
    verifyNoInteractions(contactsService)
  }

  @Test
  fun `updateVisitors throws illegal argument error when request contains update id not on visit`() {
    val visit = createVisit(officialVisitId = 3L)
    addExistingVisitor(visit, officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L, firstName = "A", lastName = "B")

    val request = OfficialVisitUpdateVisitorsRequest(listOf(existingVisitorRequest(officialVisitorId = 999L, contactId = 1L, prisonerContactId = 100L)))

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(3L, MOORLAND)).thenReturn(visit)

    val exception = assertThrows<IllegalArgumentException> {
      service.updateVisitors(3L, MOORLAND, request, MOORLAND_PRISON_USER)
    }

    assertThat(exception).hasMessage("Request contains visitors which do not exist on official visit with id 3")
    verifyNoInteractions(contactsService, auditingService, metricsService)
    assertThat(visit.officialVisitors()).hasSize(1)
  }

  @Test
  fun `updateVisitors throws illegal argument error when request contains the same visitor id twice`() {
    val visit = createVisit(officialVisitId = 4L)
    addExistingVisitor(visit, officialVisitorId = 55L, contactId = 9L, prisonerContactId = 900L, firstName = "A", lastName = "B")

    val request = OfficialVisitUpdateVisitorsRequest(
      listOf(
        existingVisitorRequest(officialVisitorId = 55L, contactId = 9L, prisonerContactId = 900L),
        existingVisitorRequest(officialVisitorId = 55L, contactId = 9L, prisonerContactId = 900L, leadVisitor = true),
      ),
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(4L, MOORLAND)).thenReturn(visit)

    val exception = assertThrows<IllegalArgumentException> {
      service.updateVisitors(4L, MOORLAND, request, MOORLAND_PRISON_USER)
    }

    assertThat(exception).hasMessage("Request contains duplicate visitors for official visit with id 4")
    verifyNoInteractions(contactsService, auditingService, metricsService)
    verify(officialVisitorRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `updateVisitors throws validation error when contactId matches but prisonerContactId differs`() {
    val visit = createVisit(officialVisitId = 5L)
    val request = OfficialVisitUpdateVisitorsRequest(listOf(newVisitorRequest(contactId = 100L, prisonerContactId = 200L)))

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(5L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true))
      .thenReturn(listOf(contact(contactId = 100L, prisonerContactId = 201L)))

    val exception = assertThrows<ValidationException> {
      service.updateVisitors(5L, MOORLAND, request, MOORLAND_PRISON_USER)
    }

    assertThat(exception).hasMessage("Invalid request: No matching prisoner contact found for contactId=100, prisonerContactId=200")
    verify(officialVisitorRepository, never()).saveAndFlush(any())
    verifyNoInteractions(auditingService, metricsService)
  }

  @Test
  fun `updateVisitors throws validation error and changes nothing when a kept visitor no longer matches a contact`() {
    val visit = createVisit(officialVisitId = 8L)
    addExistingVisitor(visit, officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L, firstName = "Ann", lastName = "Keep")
    addExistingVisitor(visit, officialVisitorId = 11L, contactId = 2L, prisonerContactId = 200L, firstName = "Bob", lastName = "Remove")

    // Visitor 10 is kept and a new visitor is added, which would otherwise remove visitor 11
    val request = OfficialVisitUpdateVisitorsRequest(
      listOf(
        existingVisitorRequest(officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L),
        newVisitorRequest(contactId = 3L, prisonerContactId = 300L),
      ),
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(8L, MOORLAND)).thenReturn(visit)
    // Contact 1's relationship no longer exists
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true))
      .thenReturn(listOf(contact(contactId = 3L, prisonerContactId = 300L)))

    val exception = assertThrows<ValidationException> {
      service.updateVisitors(8L, MOORLAND, request, MOORLAND_PRISON_USER)
    }

    assertThat(exception).hasMessage("Invalid request: No matching prisoner contact found for contactId=1, prisonerContactId=100")
    assertThat(visit.officialVisitors().map { it.officialVisitorId }).containsExactlyInAnyOrder(10L, 11L)
    verify(officialVisitorRepository, never()).saveAndFlush(any())
    verifyNoInteractions(auditingService, metricsService)
  }

  // ---------------------------------------------------------------------------------------------------------------
  // updateVisitors - adding
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `updateVisitors accepts visitor when both contactId and prisonerContactId match`() {
    val visit = createVisit(officialVisitId = 6L)
    val request = OfficialVisitUpdateVisitorsRequest(
      listOf(
        newVisitorRequest(contactId = 7L, prisonerContactId = 700L),
        newVisitorRequest(contactId = 8L, prisonerContactId = 701L),
      ),
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(6L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true)).thenReturn(
      listOf(
        contact(contactId = 7L, prisonerContactId = 700L, firstName = "Jane", lastName = "Doe"),
        contact(contactId = 8L, prisonerContactId = 701L, firstName = "Jake", lastName = "Doe"),
      ),
    )
    stubSaveVisitorReturnsArgument()

    val savedCaptor = argumentCaptor<OfficialVisitorEntity>()
    val response = service.updateVisitors(6L, MOORLAND, request, MOORLAND_PRISON_USER)

    verify(officialVisitorRepository, times(2)).saveAndFlush(savedCaptor.capture())
    assertThat(response.visitorsAdded).hasSize(2)
    assertThat(savedCaptor.firstValue.firstName).isEqualTo("Jane")
    assertThat(savedCaptor.firstValue.prisonerContactId).isEqualTo(700L)
    assertThat(savedCaptor.secondValue.firstName).isEqualTo("Jake")
    assertThat(savedCaptor.secondValue.prisonerContactId).isEqualTo(701L)
    verify(metricsService, times(2)).send(eq(MetricsEvents.ADD_VISITOR), any())
  }

  @Test
  fun `updateVisitors accepts visitor when both contactId and relationshipCode match`() {
    val visit = createVisit(officialVisitId = 6L)
    val request = OfficialVisitUpdateVisitorsRequest(listOf(newVisitorRequest(contactId = 7L, prisonerContactId = null, relationshipCode = "FRI")))

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(6L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true))
      .thenReturn(listOf(contact(contactId = 7L, prisonerContactId = 700L, firstName = "Jane", lastName = "Doe")))
    stubSaveVisitorReturnsArgument()

    val savedCaptor = argumentCaptor<OfficialVisitorEntity>()
    val response = service.updateVisitors(6L, MOORLAND, request, MOORLAND_PRISON_USER)

    verify(officialVisitorRepository).saveAndFlush(savedCaptor.capture())
    assertThat(response.visitorsAdded).hasSize(1)
    assertThat(savedCaptor.firstValue.firstName).isEqualTo("Jane")
    assertThat(savedCaptor.firstValue.prisonerContactId).isEqualTo(700L)
  }

  // ---------------------------------------------------------------------------------------------------------------
  // updateVisitors - updating
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `updateVisitors does not save or audit an existing visitor that has not changed`() {
    val visit = createVisit(officialVisitId = 12L)
    addExistingVisitor(visit, officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L, firstName = "Ann", lastName = "Same")

    // leadVisitor and assistedVisit are null in the request and false on the record - not a change
    val request = OfficialVisitUpdateVisitorsRequest(
      listOf(existingVisitorRequest(officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L, leadVisitor = null, assistedVisit = null)),
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(12L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true))
      .thenReturn(listOf(contact(contactId = 1L, prisonerContactId = 100L, firstName = "Ann", lastName = "Same")))

    val response = service.updateVisitors(12L, MOORLAND, request, MOORLAND_PRISON_USER)

    assertThat(response.visitorsAdded).isEmpty()
    assertThat(response.visitorsUpdated).isEmpty()
    assertThat(response.visitorsDeleted).isEmpty()
    verify(officialVisitorRepository, never()).saveAndFlush(any())
    assertThat(capturedVisitorAudit().detailText).doesNotContain("Ann Same")
  }

  @Test
  fun `updateVisitors updates an existing visitor when the request changes it`() {
    val visit = createVisit(officialVisitId = 13L)
    val existing = addExistingVisitor(visit, officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L, firstName = "Bob", lastName = "Change")

    val request = OfficialVisitUpdateVisitorsRequest(
      listOf(
        existingVisitorRequest(
          officialVisitorId = 10L,
          contactId = 1L,
          prisonerContactId = 100L,
          leadVisitor = true,
          assistedVisit = true,
          assistedNotes = "wheelchair",
        ),
      ),
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(13L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true))
      .thenReturn(listOf(contact(contactId = 1L, prisonerContactId = 100L, firstName = "Bob", lastName = "Change")))
    stubSaveVisitorReturnsArgument()

    val response = service.updateVisitors(13L, MOORLAND, request, MOORLAND_PRISON_USER)

    assertThat(response.visitorsUpdated.map { it.officialVisitorId }).containsExactly(10L)
    with(existing) {
      assertThat(leadVisitor).isTrue
      assertThat(assistedVisit).isTrue
      assertThat(visitorNotes).isEqualTo("wheelchair")
      assertThat(updatedBy).isEqualTo(MOORLAND_PRISON_USER.username)
      assertThat(updatedTime).isNotNull
    }
    verify(officialVisitorRepository).saveAndFlush(existing)
    verify(officialVisitorRepository, never()).findById(any())
    assertThat(capturedVisitorAudit().detailText).contains("Bob Change")
  }

  @Test
  fun `updateVisitors clears equipment when it is no longer in the request`() {
    val visit = createVisit(officialVisitId = 14L)
    val existing = addExistingVisitor(
      visit,
      officialVisitorId = 10L,
      contactId = 1L,
      prisonerContactId = 100L,
      firstName = "Eve",
      lastName = "Kit",
      equipmentDescription = "Laptop",
    )

    val request = OfficialVisitUpdateVisitorsRequest(listOf(existingVisitorRequest(officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L)))

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(14L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true))
      .thenReturn(listOf(contact(contactId = 1L, prisonerContactId = 100L, firstName = "Eve", lastName = "Kit")))
    stubSaveVisitorReturnsArgument()

    val response = service.updateVisitors(14L, MOORLAND, request, MOORLAND_PRISON_USER)

    assertThat(response.visitorsUpdated.map { it.officialVisitorId }).containsExactly(10L)
    assertThat(existing.visitorEquipment).isNull()
  }

  // ---------------------------------------------------------------------------------------------------------------
  // updateVisitors - removing and the combined flow
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `updateVisitors removes a visitor whose contact relationship no longer exists without failing the audit`() {
    val visit = createVisit(officialVisitId = 15L)
    addExistingVisitor(visit, officialVisitorId = 12L, contactId = 3L, prisonerContactId = 300L, firstName = "Cat", lastName = "Gone")

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(15L, MOORLAND)).thenReturn(visit)
    // The removed visitor's contact is not returned
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true)).thenReturn(emptyList())

    val response = service.updateVisitors(15L, MOORLAND, OfficialVisitUpdateVisitorsRequest(emptyList()), MOORLAND_PRISON_USER)

    assertThat(response.visitorsDeleted.map { it.officialVisitorId }).containsExactly(12L)
    assertThat(visit.officialVisitors()).isEmpty()
    assertThat(capturedVisitorAudit().detailText).contains("Cat Gone")
    verify(metricsService).send(eq(MetricsEvents.REMOVE_VISITOR), any())
  }

  @Test
  fun `updateVisitors adds, updates, keeps and removes visitors in a single request`() {
    val visit = createVisit(officialVisitId = 20L)
    addExistingVisitor(visit, officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L, firstName = "Ann", lastName = "Keep")
    val toUpdate = addExistingVisitor(visit, officialVisitorId = 11L, contactId = 2L, prisonerContactId = 200L, firstName = "Bob", lastName = "Change")
    addExistingVisitor(visit, officialVisitorId = 12L, contactId = 3L, prisonerContactId = 300L, firstName = "Cat", lastName = "Gone")

    val request = OfficialVisitUpdateVisitorsRequest(
      listOf(
        existingVisitorRequest(officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L),
        existingVisitorRequest(officialVisitorId = 11L, contactId = 2L, prisonerContactId = 200L, leadVisitor = true),
        newVisitorRequest(contactId = 4L, prisonerContactId = 400L),
      ),
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(20L, MOORLAND)).thenReturn(visit)
    // Contact 3 (being removed) is no longer a current contact
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true)).thenReturn(
      listOf(
        contact(contactId = 1L, prisonerContactId = 100L, firstName = "Ann", lastName = "Keep"),
        contact(contactId = 2L, prisonerContactId = 200L, firstName = "Bob", lastName = "Change"),
        contact(contactId = 4L, prisonerContactId = 400L, firstName = "DAVE", lastName = "NEW"),
      ),
    )
    stubSaveVisitorReturnsArgument()

    val response = service.updateVisitors(20L, MOORLAND, request, MOORLAND_PRISON_USER)

    assertThat(response.visitorsAdded.map { it.contactId }).containsExactly(4L)
    assertThat(response.visitorsUpdated.map { it.officialVisitorId }).containsExactly(11L)
    assertThat(response.visitorsDeleted.map { it.officialVisitorId }).containsExactly(12L)

    assertThat(visit.officialVisitors().map { it.officialVisitorId }).containsExactlyInAnyOrder(10L, 11L)
    assertThat(toUpdate.leadVisitor).isTrue

    val savedCaptor = argumentCaptor<OfficialVisitorEntity>()
    verify(officialVisitorRepository, times(2)).saveAndFlush(savedCaptor.capture())
    assertThat(savedCaptor.allValues.map { it.contactId }).containsExactlyInAnyOrder(2L, 4L)

    with(capturedVisitorAudit()) {
      officialVisitId isEqualTo 20
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      prisonCode isEqualTo MOORLAND
      eventSource isEqualTo "DPS"
      username isEqualTo MOORLAND_PRISON_USER.username
      assertThat(detailText).contains("Dave New", "Bob Change", "Cat Gone")
      assertThat(detailText).doesNotContain("Ann Keep")
    }

    verify(metricsService).send(eq(MetricsEvents.ADD_VISITOR), any())
    verify(metricsService).send(eq(MetricsEvents.REMOVE_VISITOR), any())
  }

  // ---------------------------------------------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------------------------------------------

  private fun capturedVisitorAudit(): AuditEventDto {
    val captor = argumentCaptor<AuditEventDto>()
    verify(auditingService).recordAuditEvent(captor.capture())
    return captor.firstValue
  }

  private fun stubSaveVisitorReturnsArgument() {
    whenever(officialVisitorRepository.saveAndFlush(any<OfficialVisitorEntity>())).thenAnswer { it.arguments[0] as OfficialVisitorEntity }
  }

  // Default relationship code matches the prisonerContact helper's default ("FRI") so kept visitors are unchanged
  private fun contact(
    contactId: Long,
    prisonerContactId: Long,
    firstName: String = "First",
    lastName: String = "Last",
  ): PrisonerContact = prisonerContact(
    prisonerNumber = MOORLAND_PRISONER.number,
    type = "O",
    contactId = contactId,
    prisonerContactId = prisonerContactId,
    firstName = firstName,
    lastName = lastName,
  ).toPrisonerContactModel()

  private fun newVisitorRequest(contactId: Long, prisonerContactId: Long?, relationshipCode: String = "POM") = OfficialVisitor(
    officialVisitorId = 0L,
    visitorTypeCode = VisitorType.CONTACT,
    contactId = contactId,
    prisonerContactId = prisonerContactId,
    relationshipCode = relationshipCode,
  )

  private fun existingVisitorRequest(
    officialVisitorId: Long,
    contactId: Long,
    prisonerContactId: Long,
    leadVisitor: Boolean? = false,
    assistedVisit: Boolean? = false,
    assistedNotes: String? = null,
  ) = OfficialVisitor(
    officialVisitorId = officialVisitorId,
    visitorTypeCode = VisitorType.CONTACT,
    contactId = contactId,
    prisonerContactId = prisonerContactId,
    relationshipCode = "FRI",
    leadVisitor = leadVisitor,
    assistedVisit = assistedVisit,
    assistedNotes = assistedNotes,
  )

  private fun createSlot(id: Long) = PrisonVisitSlotEntity(
    prisonVisitSlotId = id,
    prisonTimeSlotId = id,
    dpsLocationId = UUID.randomUUID(),
    createdTime = LocalDateTime.now(),
    createdBy = "test",
  )

  private fun createVisit(officialVisitId: Long): OfficialVisitEntity {
    val slot = createSlot(1L)
    return OfficialVisitEntity(
      officialVisitId = officialVisitId,
      prisonVisitSlot = slot,
      visitDate = LocalDate.now().plusDays(1),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationId = slot.dpsLocationId,
      visitTypeCode = VisitType.IN_PERSON,
      prisonCode = MOORLAND,
      prisonerNumber = MOORLAND_PRISONER.number,
      createdBy = "test",
      createdTime = LocalDateTime.now().minusDays(1),
    )
  }

  /**
   * Adds a visitor to the visit and returns the entity actually held on the visit, with its id, notes and
   * equipment set, so assertions run against the same object the service changes.
   */
  private fun addExistingVisitor(
    visit: OfficialVisitEntity,
    officialVisitorId: Long,
    contactId: Long,
    prisonerContactId: Long,
    firstName: String,
    lastName: String,
    relationshipCode: String = "FRI",
    leadVisitor: Boolean = false,
    assistedVisit: Boolean = false,
    assistedNotes: String? = null,
    equipmentDescription: String? = null,
  ): OfficialVisitorEntity {
    visit.addVisitor(
      visitorTypeCode = VisitorType.CONTACT,
      firstName = firstName,
      lastName = lastName,
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      relationshipTypeCode = RelationshipType.OFFICIAL,
      relationshipCode = relationshipCode,
      leadVisitor = leadVisitor,
      assistedVisit = assistedVisit,
      createdBy = MOORLAND_PRISON_USER,
      createdTime = LocalDateTime.now().minusHours(1),
    )

    return visit.officialVisitors().single { it.contactId == contactId && it.prisonerContactId == prisonerContactId }.apply {
      ReflectionTestUtils.setField(this, "officialVisitorId", officialVisitorId)
      visitorNotes = assistedNotes
      if (!equipmentDescription.isNullOrBlank()) {
        visitorEquipment = VisitorEquipmentEntity(officialVisitor = this, description = equipmentDescription, createdBy = "test")
      }
    }
  }
}
