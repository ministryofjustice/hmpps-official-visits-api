package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.RestrictionsSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitorEquipmentEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitorEquipmentRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.VisitorMetricInfo
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class SyncOfficialVisitorServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val officialVisitorRepository: OfficialVisitorRepository = mock()
  private val contactsService: ContactsService = mock()
  private val visitorEquipmentRepository: VisitorEquipmentRepository = mock()
  private val metricsService: MetricsService = mock()

  private val createdTime = LocalDateTime.now().minusDays(2)

  private val syncOfficialVisitService = SyncOfficialVisitorService(
    officialVisitRepository,
    officialVisitorRepository,
    contactsService,
    visitorEquipmentRepository,
    metricsService,
  )

  @AfterEach
  fun afterEach() {
    reset(officialVisitRepository, officialVisitorRepository, contactsService, visitorEquipmentRepository, metricsService)
  }

  @Test
  fun `add a visitor - should succeed`() {
    val visitSlotId = 1L
    val visitId = 2L
    val visitorId = 3L
    val contactId = 4L
    val prisonerContactId = 5L
    val offenderVisitVisitorId = 6L

    // Mocked visit
    val visitSlotEntity = prisonVisitSlotEntity(visitSlotId)
    val visitRequest = createOfficialVisitRequest(visitId)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, visitRequest)

    // Mocked visitor
    val officialVisitorEntity = officialVisitorEntity(officialVisitEntity, visitorId, contactId, prisonerContactId, offenderVisitVisitorId)

    val visitorRequest = createVisitorRequest(offenderVisitVisitorId, contactId)

    whenever(officialVisitRepository.findById(visitId)).thenReturn(Optional.of(officialVisitEntity))
    whenever(contactsService.getPrisonerContactSummary(MOORLAND_PRISONER.number, contactId)).thenReturn(
      listOf(prisonerContactSummary(prisonerContactId, MOORLAND_PRISONER.number, contactId)),
    )
    whenever(officialVisitorRepository.saveAndFlush(any())).thenReturn(officialVisitorEntity)

    val result = syncOfficialVisitService.createVisitor(visitId, visitorRequest)

    with(result) {
      assertThat(prisonCode).isEqualTo(officialVisitEntity.prisonCode)
      assertThat(prisonerNumber).isEqualTo(officialVisitEntity.prisonerNumber)
      assertThat(officialVisitId).isEqualTo(officialVisitEntity.officialVisitId)
      assertThat(officialVisitorId).isEqualTo(officialVisitorEntity.officialVisitorId)

      assertThat(visitor.contactId).isEqualTo(contactId)
      assertThat(visitor.officialVisitorId).isEqualTo(officialVisitorId)
      assertThat(visitor.relationshipType).isEqualTo(RelationshipType.OFFICIAL)
      assertThat(visitor.relationshipCode).isEqualTo("POL")
    }

    verify(officialVisitRepository).findById(visitId)
    verify(contactsService).getPrisonerContactSummary(MOORLAND_PRISONER.number, contactId)
    verify(officialVisitorRepository).saveAndFlush(any())
    verify(metricsService).send(
      MetricsEvents.ADD_VISITOR,
      VisitorMetricInfo(
        source = Source.NOMIS,
        username = "Test",
        prisonCode = MOORLAND,
        officialVisitId = result.officialVisitId,
        contactId = result.visitor.contactId!!,
        officialVisitorId = result.visitor.officialVisitorId,
      ),
    )
  }

  @Test
  fun `add a visitor - should fail if the visit ID was not found`() {
    val visitId = 2L
    val contactId = 4L
    val offenderVisitVisitorId = 6L

    val visitorRequest = createVisitorRequest(offenderVisitVisitorId, contactId)

    whenever(officialVisitRepository.findById(visitId)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.createVisitor(visitId, visitorRequest)
    }

    verify(officialVisitRepository).findById(visitId)
    verifyNoInteractions(contactsService, officialVisitorRepository, metricsService)
  }

  @Test
  fun `add a visitor - should fail when it would cause a duplicate visitor`() {
    val visitSlotId = 1L
    val visitId = 2L
    val contactId = 4L
    val offenderVisitVisitorId = 6L

    // Mocked visit
    val visitSlotEntity = prisonVisitSlotEntity(visitSlotId)
    val visitRequest = createOfficialVisitRequest(visitId)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, visitRequest)

    val visitorRequest = createVisitorRequest(offenderVisitVisitorId, contactId)

    // Before calling the service add a duplicate visitor to the visit
    officialVisitEntity.addVisitor(
      visitorTypeCode = VisitorType.CONTACT,
      relationshipTypeCode = visitorRequest.relationshipTypeCode!!,
      relationshipCode = visitorRequest.relationshipToPrisoner!!,
      contactId = visitorRequest.personId,
      firstName = visitorRequest.firstName,
      lastName = visitorRequest.lastName,
      leadVisitor = visitorRequest.groupLeaderFlag!!,
      assistedVisit = visitorRequest.assistedVisitFlag!!,
      assistedNotes = visitorRequest.commentText,
      createdBy = MOORLAND_PRISON_USER,
      createdTime = visitorRequest.createDateTime!!,
    )

    whenever(officialVisitRepository.findById(visitId)).thenReturn(Optional.of(officialVisitEntity))

    assertThrows<EntityInUseException> {
      syncOfficialVisitService.createVisitor(visitId, visitorRequest)
    }

    verify(officialVisitRepository).findById(visitId)
    verifyNoInteractions(contactsService, officialVisitorRepository, metricsService)
  }

  @Test
  fun `remove a visitor - should remove a visitor from a visit`() {
    val visitSlotId = 1L
    val officialVisitId = 0L
    val officialVisitorId = 0L
    val contactId = 4L
    val prisonerContactId = 5L

    // Create a visit entity
    val visitSlotEntity = prisonVisitSlotEntity(visitSlotId)
    val visitRequest = createOfficialVisitRequest(visitSlotId)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, visitRequest)

    // Add a visitor to the visit entity
    val officialVisitorEntity = officialVisitEntity.addVisitor(
      visitorTypeCode = VisitorType.CONTACT,
      relationshipTypeCode = RelationshipType.OFFICIAL,
      relationshipCode = "POL",
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      createdBy = MOORLAND_PRISON_USER,
      createdTime = LocalDateTime.now(),
    )

    // Add equipment to the visitor entity
    officialVisitorEntity.visitorEquipment = VisitorEquipmentEntity(
      visitorEquipmentId = 1L,
      officialVisitor = officialVisitorEntity,
      description = "laptop",
      createdBy = MOORLAND_PRISON_USER.username,
    )

    whenever(officialVisitRepository.findById(officialVisitId)).thenReturn(Optional.of(officialVisitEntity))

    val response = syncOfficialVisitService.deleteVisitor(officialVisitId, officialVisitorId)

    with(response) {
      assertThat(officialVisitId).isEqualTo(officialVisitId)
      assertThat(officialVisitorId).isEqualTo(officialVisitorId)
      assertThat(this?.prisonCode).isEqualTo(MOORLAND)
      assertThat(this?.prisonerNumber).isEqualTo(MOORLAND_PRISONER.number)
      assertThat(contactId).isEqualTo(contactId)
    }

    verify(officialVisitRepository).findById(officialVisitId)
    verify(visitorEquipmentRepository).deleteAllByOfficialVisitor(officialVisitorEntity)
    verify(metricsService).send(
      MetricsEvents.REMOVE_VISITOR,
      VisitorMetricInfo(
        source = Source.NOMIS,
        username = "NOMIS",
        prisonCode = MOORLAND,
        officialVisitId = response?.officialVisitId!!,
        contactId = response.contactId!!,
        officialVisitorId = response.officialVisitorId,
      ),
    )
  }

  @Test
  fun `remove a visitor - should silently succeed if the visit was not found`() {
    val officialVisitId = 1L
    val officialVisitorId = 2L

    whenever(officialVisitRepository.findById(officialVisitId)).thenReturn(Optional.empty())

    val response = syncOfficialVisitService.deleteVisitor(officialVisitId, officialVisitorId)

    assertThat(response).isNull()
  }

  @Test
  fun `remove a visitor - should silently succeed if the visitor was not found on the visit`() {
    val officialVisitId = 1L
    val officialVisitorId = 2L
    val visitSlotId = 1L

    // Create a visit entity - with no visitors
    val visitSlotEntity = prisonVisitSlotEntity(visitSlotId)
    val visitRequest = createOfficialVisitRequest(visitSlotId)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, visitRequest)

    whenever(officialVisitRepository.findById(officialVisitId)).thenReturn(Optional.of(officialVisitEntity))

    val response = syncOfficialVisitService.deleteVisitor(officialVisitId, officialVisitorId)
    assertThat(response).isNull()
  }

  @Test
  fun `update a visitor - should succeed`() {
    val visitSlotId = 1L
    val visitId = 2L
    val visitorId = 3L
    val contactId = 4L
    val prisonerContactId = 5L
    val offenderVisitVisitorId = 6L

    // Mocked visit
    val visitSlotEntity = prisonVisitSlotEntity(visitSlotId)
    val visitRequest = createOfficialVisitRequest(visitId)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, visitRequest)

    // Mocked visitor
    val officialVisitorEntity = officialVisitorEntity(officialVisitEntity, visitorId, contactId, prisonerContactId, offenderVisitVisitorId)

    // Update visitor request
    val visitorUpdateRequest = updateVisitorRequest(
      offenderVisitVisitorId = offenderVisitVisitorId,
      contactId = contactId,
      firstName = "FirstX",
      lastName = "LastX",
      relationshipToPrisoner = "POM",
      groupLeaderFlag = false,
      assistedVisitFlag = false,
      commentText = "Changed",
    )

    whenever(officialVisitRepository.findById(visitId)).thenReturn(Optional.of(officialVisitEntity))
    whenever(officialVisitorRepository.findById(visitorId)).thenReturn(Optional.of(officialVisitorEntity))
    whenever(officialVisitorRepository.saveAndFlush(any())).thenReturn(officialVisitorEntity)

    syncOfficialVisitService.updateVisitor(visitId, visitorId, visitorUpdateRequest)

    verify(officialVisitRepository).findById(visitId)
    verify(officialVisitorRepository).findById(visitorId)

    val visitorCaptor = argumentCaptor<OfficialVisitorEntity>()
    verify(officialVisitorRepository).saveAndFlush(visitorCaptor.capture())

    with(visitorCaptor.firstValue) {
      // Check that the update request changes are saved
      assertThat(officialVisitorId).isEqualTo(visitorId)
      assertThat(firstName).isEqualTo(visitorUpdateRequest.firstName)
      assertThat(lastName).isEqualTo(visitorUpdateRequest.lastName)
      assertThat(relationshipCode).isEqualTo(visitorUpdateRequest.relationshipToPrisoner)
      assertThat(leadVisitor).isEqualTo(visitorUpdateRequest.groupLeaderFlag)
      assertThat(assistedVisit).isEqualTo(visitorUpdateRequest.assistedVisitFlag)
      assertThat(visitorNotes).isEqualTo(visitorUpdateRequest.commentText)
      assertThat(attendanceCode).isEqualTo(visitorUpdateRequest.attendanceCode)
    }
  }

  @Test
  fun `update a visitor - should fail if the visit does not exist`() {
    val visitId = 2L
    val visitorId = 3L
    val contactId = 4L
    val offenderVisitVisitorId = 6L

    val visitorUpdateRequest = updateVisitorRequest(
      offenderVisitVisitorId = offenderVisitVisitorId,
      contactId = contactId,
      firstName = "FirstX",
      lastName = "LastX",
      relationshipToPrisoner = "POL",
      groupLeaderFlag = true,
      assistedVisitFlag = true,
      commentText = "Changed",
    )

    whenever(officialVisitRepository.findById(visitId)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.updateVisitor(visitId, visitorId, visitorUpdateRequest)
    }

    exception.message isEqualTo "The official visit with id $visitId was not found"

    verify(officialVisitRepository).findById(visitId)
    verifyNoInteractions(officialVisitorRepository, metricsService)
  }

  @Test
  fun `update a visitor - should fail if the visitor does not exist`() {
    val visitSlotId = 1L
    val visitId = 2L
    val visitorId = 3L
    val contactId = 4L
    val offenderVisitVisitorId = 6L

    // Mocked visit
    val visitSlotEntity = prisonVisitSlotEntity(visitSlotId)
    val visitRequest = createOfficialVisitRequest(visitId)
    val officialVisitEntity = OfficialVisitEntity.synchronised(visitSlotEntity, visitRequest)

    // Update request
    val visitorUpdateRequest = updateVisitorRequest(
      offenderVisitVisitorId = offenderVisitVisitorId,
      contactId = contactId,
      firstName = "FirstX",
      lastName = "LastX",
      relationshipToPrisoner = "POL",
      groupLeaderFlag = true,
      assistedVisitFlag = true,
      commentText = "Changed",
    )

    whenever(officialVisitRepository.findById(visitId)).thenReturn(Optional.of(officialVisitEntity))
    whenever(officialVisitorRepository.findById(visitorId)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      syncOfficialVisitService.updateVisitor(visitId, visitorId, visitorUpdateRequest)
    }

    exception.message isEqualTo "The official visitor with id $visitorId was not found"

    verify(officialVisitRepository).findById(visitId)
    verify(officialVisitorRepository).findById(visitorId)
    verify(officialVisitorRepository, never()).saveAndFlush(any())
    verifyNoInteractions(metricsService)
  }

  private fun createOfficialVisitRequest(visitSlotId: Long) = SyncCreateOfficialVisitRequest(
    offenderVisitId = 1L,
    prisonVisitSlotId = visitSlotId,
    prisonCode = MOORLAND,
    offenderBookId = 1L,
    prisonerNumber = MOORLAND_PRISONER.number,
    visitDate = tomorrow(),
    startTime = LocalTime.parse("09:00"),
    endTime = LocalTime.parse("10:00"),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    createDateTime = createdTime,
    createUsername = "Bob",
  )

  private fun prisonVisitSlotEntity(prisonVisitSlotId: Long = 1L) = PrisonVisitSlotEntity(
    prisonVisitSlotId = prisonVisitSlotId,
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    createdBy = "Test",
    createdTime = createdTime,
  )

  private fun createVisitorRequest(offenderVisitVisitorId: Long, contactId: Long) = SyncCreateOfficialVisitorRequest(
    offenderVisitVisitorId = offenderVisitVisitorId,
    personId = contactId,
    firstName = "First",
    lastName = "Last",
    relationshipTypeCode = RelationshipType.OFFICIAL,
    relationshipToPrisoner = "POL",
    groupLeaderFlag = false,
    assistedVisitFlag = false,
    commentText = "Notes",
    createDateTime = createdTime,
    createUsername = "Bob",
  )

  private fun updateVisitorRequest(
    offenderVisitVisitorId: Long,
    contactId: Long,
    firstName: String,
    lastName: String,
    relationshipToPrisoner: String,
    groupLeaderFlag: Boolean,
    assistedVisitFlag: Boolean,
    commentText: String,
  ) = SyncUpdateOfficialVisitorRequest(
    offenderVisitVisitorId = offenderVisitVisitorId,
    personId = contactId,
    firstName = firstName,
    lastName = lastName,
    relationshipTypeCode = RelationshipType.OFFICIAL,
    relationshipToPrisoner = relationshipToPrisoner,
    groupLeaderFlag = groupLeaderFlag,
    assistedVisitFlag = assistedVisitFlag,
    commentText = commentText,
    attendanceCode = AttendanceType.ATTENDED,
    updateUsername = MOORLAND_PRISON_USER.username,
    updateDateTime = LocalDateTime.now().minusMinutes(10),
  )

  private fun prisonerContactSummary(
    prisonerContactId: Long,
    prisonerNumber: String,
    contactId: Long,
  ) = PrisonerContactSummary(
    prisonerContactId = prisonerContactId,
    contactId = contactId,
    prisonerNumber = prisonerNumber,
    lastName = "Last",
    firstName = "First",
    relationshipTypeCode = "OFFICIAL",
    relationshipTypeDescription = "Official",
    relationshipToPrisonerCode = "POL",
    relationshipToPrisonerDescription = "Police officer",
    isApprovedVisitor = true,
    isNextOfKin = false,
    isEmergencyContact = false,
    isRelationshipActive = true,
    currentTerm = true,
    isStaff = false,
    restrictionSummary = RestrictionsSummary(
      active = emptySet(),
      totalActive = 0,
      totalExpired = 0,
    ),
  )

  private fun officialVisitorEntity(
    officialVisit: OfficialVisitEntity,
    visitorId: Long,
    contactId: Long,
    prisonerContactId: Long,
    offenderVisitVisitorId: Long,
  ) = OfficialVisitorEntity(
    officialVisitorId = visitorId,
    officialVisit = officialVisit,
    visitorTypeCode = VisitorType.CONTACT,
    firstName = "First",
    lastName = "Last",
    contactId = contactId,
    prisonerContactId = prisonerContactId,
    relationshipTypeCode = RelationshipType.OFFICIAL,
    relationshipCode = "POL",
    leadVisitor = false,
    assistedVisit = false,
    visitorNotes = "Notes",
    offenderVisitVisitorId = offenderVisitVisitorId,
    createdBy = "Test",
    createdTime = LocalDateTime.now(),
  )
}
