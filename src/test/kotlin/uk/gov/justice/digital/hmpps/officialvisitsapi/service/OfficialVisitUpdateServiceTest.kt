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
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitorEquipmentEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toPrisonerContactModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateCommentRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
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

  private val service = OfficialVisitUpdateService(
    officialVisitRepository,
    officialVisitorRepository,
    prisonVisitSlotRepository,
    contactsService,
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

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(11L, MOORLAND)).thenReturn(visit)
    whenever(prisonVisitSlotRepository.findById(99L)).thenReturn(Optional.of(newSlot))
    whenever(officialVisitRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] as OfficialVisitEntity }

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
    verify(officialVisitRepository, times(1)).saveAndFlush(visit)
  }

  @Test
  fun `updateVisitTypeAndSlot throws when visit not found`() {
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
  fun `updateVisitTypeAndSlot throws when prison visit slot not found`() {
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
    whenever(officialVisitRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] as OfficialVisitEntity }

    val response = service.updateComments(7L, MOORLAND, request, MOORLAND_PRISON_USER)

    assertThat(response.officialVisitId).isEqualTo(7L)
    assertThat(response.prisonerNumber).isEqualTo(MOORLAND_PRISONER.number)
    assertThat(visit.staffNotes).isEqualTo("staff updated")
    assertThat(visit.prisonerNotes).isEqualTo("prisoner updated")
    assertThat(visit.updatedBy).isEqualTo(MOORLAND_PRISON_USER.username)
    assertThat(visit.updatedTime).isNotNull
    verify(officialVisitRepository, times(1)).saveAndFlush(visit)
  }

  @Test
  fun `updateComments throws when visit not found`() {
    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(9L, MOORLAND)).thenReturn(null)

    assertThrows<EntityNotFoundException> {
      service.updateComments(9L, MOORLAND, OfficialVisitUpdateCommentRequest("a", "b"), MOORLAND_PRISON_USER)
    }
    verify(officialVisitRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `updateVisitors adds updates and removes visitors in one request`() {
    val visit = createVisit(officialVisitId = 1L)
    val updateVisitor = addExistingVisitor(visit, officialVisitorId = 10L, contactId = 2L, prisonerContactId = 200L, firstName = "Old", lastName = "Name", relationshipCode = "OLD")
    val removedVisitor = addExistingVisitor(visit, officialVisitorId = 20L, contactId = 3L, prisonerContactId = 300L, firstName = "Remove", lastName = "Me", relationshipCode = "DEL")

    val request = OfficialVisitUpdateVisitorsRequest(
      officialVisitors = listOf(
        OfficialVisitor(
          officialVisitorId = 0L,
          visitorTypeCode = VisitorType.CONTACT,
          contactId = 4L,
          prisonerContactId = 400L,
          relationshipCode = "POM",
          leadVisitor = true,
          assistedVisit = true,
          assistedNotes = "needs support",
          visitorEquipment = VisitorEquipment("laptop"),
        ),
        OfficialVisitor(
          officialVisitorId = 10L,
          visitorTypeCode = VisitorType.CONTACT,
          contactId = 2L,
          prisonerContactId = 200L,
          relationshipCode = "POM",
          leadVisitor = false,
          assistedVisit = false,
          assistedNotes = null,
          visitorEquipment = VisitorEquipment("tablet"),
        ),
      ),
    )

    val contacts = listOf(
      prisonerContact(
        prisonerNumber = MOORLAND_PRISONER.number,
        type = "O",
        contactId = 4L,
        prisonerContactId = 400L,
        firstName = "Add",
        lastName = "Visitor",
      ).toPrisonerContactModel(),
      prisonerContact(
        prisonerNumber = MOORLAND_PRISONER.number,
        type = "S",
        contactId = 2L,
        prisonerContactId = 200L,
        firstName = "Updated",
        lastName = "Person",
      ).toPrisonerContactModel(),
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(1L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true)).thenReturn(contacts)
    whenever(officialVisitorRepository.findById(10L)).thenReturn(Optional.of(updateVisitor))
    whenever(officialVisitorRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] as OfficialVisitorEntity }

    val saveCaptor = argumentCaptor<OfficialVisitorEntity>()

    val response = service.updateVisitors(1L, MOORLAND, request, MOORLAND_PRISON_USER)

    verify(officialVisitorRepository, times(2)).saveAndFlush(saveCaptor.capture())

    assertThat(response.visitorsAdded).hasSize(1)
    assertThat(response.visitorsUpdated).hasSize(1)
    assertThat(response.visitorsDeleted).hasSize(1)
    assertThat(response.visitorsDeleted.first().officialVisitorId).isEqualTo(20L)
    assertThat(response.visitorsDeleted.first().contactId).isEqualTo(removedVisitor.contactId)

    val savedNewVisitor = saveCaptor.allValues.single { it.contactId == 4L }
    assertThat(savedNewVisitor.firstName).isEqualTo("Add")
    assertThat(savedNewVisitor.lastName).isEqualTo("Visitor")
    assertThat(savedNewVisitor.prisonerContactId).isEqualTo(400L)

    val savedUpdatedVisitor = saveCaptor.allValues.single { it.officialVisitorId == 10L }
    assertThat(savedUpdatedVisitor.relationshipTypeCode).isEqualTo(RelationshipType.SOCIAL)
    assertThat(savedUpdatedVisitor.relationshipCode).isEqualTo("FRI")
    assertThat(savedUpdatedVisitor.updatedBy).isEqualTo(MOORLAND_PRISON_USER.username)
  }

  @Test
  fun `updateVisitors does not save unchanged existing visitor`() {
    val visit = createVisit(officialVisitId = 2L)
    val existing = addExistingVisitor(
      visit = visit,
      officialVisitorId = 30L,
      contactId = 5L,
      prisonerContactId = 500L,
      firstName = "Same",
      lastName = "Person",
      relationshipCode = "POM",
      leadVisitor = false,
      assistedVisit = false,
      assistedNotes = "same notes",
      equipmentDescription = "wheelchair",
    )

    val request = OfficialVisitUpdateVisitorsRequest(
      officialVisitors = listOf(
        OfficialVisitor(
          officialVisitorId = 30L,
          visitorTypeCode = VisitorType.CONTACT,
          contactId = 5L,
          prisonerContactId = 500L,
          relationshipCode = "POM",
          leadVisitor = false,
          assistedVisit = false,
          assistedNotes = "same notes",
          visitorEquipment = VisitorEquipment("wheelchair"),
        ),
      ),
    )

    val matchingContact = prisonerContact(
      prisonerNumber = MOORLAND_PRISONER.number,
      type = "O",
      contactId = 5L,
      prisonerContactId = 500L,
      firstName = "Same",
      lastName = "Person",
    ).toPrisonerContactModel()

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(2L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true)).thenReturn(listOf(matchingContact))
    whenever(officialVisitorRepository.findById(30L)).thenReturn(Optional.of(existing))

    val response = service.updateVisitors(2L, MOORLAND, request, MOORLAND_PRISON_USER)

    assertThat(response.visitorsAdded).isEmpty()
    assertThat(response.visitorsDeleted).isEmpty()
    assertThat(response.visitorsUpdated).isEmpty()
    verify(officialVisitorRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `updateVisitors throws when visit not found`() {
    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(99L, MOORLAND)).thenReturn(null)

    assertThrows<EntityNotFoundException> {
      service.updateVisitors(99L, MOORLAND, OfficialVisitUpdateVisitorsRequest(emptyList()), MOORLAND_PRISON_USER)
    }
    verifyNoInteractions(contactsService)
  }

  @Test
  fun `updateVisitors throws when request contains update id not on visit`() {
    val visit = createVisit(officialVisitId = 3L)
    addExistingVisitor(visit, officialVisitorId = 10L, contactId = 1L, prisonerContactId = 100L, firstName = "A", lastName = "B", relationshipCode = "POM")

    val request = OfficialVisitUpdateVisitorsRequest(
      officialVisitors = listOf(
        OfficialVisitor(
          officialVisitorId = 999L,
          visitorTypeCode = VisitorType.CONTACT,
          contactId = 1L,
          prisonerContactId = 100L,
          relationshipCode = "POM",
        ),
      ),
    )

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(3L, MOORLAND)).thenReturn(visit)

    assertThrows<IllegalArgumentException> {
      service.updateVisitors(3L, MOORLAND, request, MOORLAND_PRISON_USER)
    }
    verifyNoInteractions(contactsService)
  }

  @Test
  fun `updateVisitors throws when existing visitor cannot be loaded for update`() {
    val visit = createVisit(officialVisitId = 4L)
    addExistingVisitor(visit, officialVisitorId = 55L, contactId = 9L, prisonerContactId = 900L, firstName = "A", lastName = "B", relationshipCode = "POM")

    val request = OfficialVisitUpdateVisitorsRequest(
      officialVisitors = listOf(
        OfficialVisitor(
          officialVisitorId = 55L,
          visitorTypeCode = VisitorType.CONTACT,
          contactId = 9L,
          prisonerContactId = 900L,
          relationshipCode = "POM",
        ),
      ),
    )

    val matchingContact = prisonerContact(
      prisonerNumber = MOORLAND_PRISONER.number,
      type = "O",
      contactId = 9L,
      prisonerContactId = 900L,
    ).toPrisonerContactModel()

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(4L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true)).thenReturn(listOf(matchingContact))
    whenever(officialVisitorRepository.findById(55L)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      service.updateVisitors(4L, MOORLAND, request, MOORLAND_PRISON_USER)
    }
    verify(officialVisitorRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `updateVisitors throws validation error when contactId matches but prisonerContactId differs`() {
    val visit = createVisit(officialVisitId = 5L)
    val request = OfficialVisitUpdateVisitorsRequest(
      officialVisitors = listOf(
        OfficialVisitor(
          officialVisitorId = 0L,
          visitorTypeCode = VisitorType.CONTACT,
          contactId = 100L,
          prisonerContactId = 200L,
          relationshipCode = "POM",
        ),
      ),
    )

    val returnedContact = prisonerContact(
      prisonerNumber = MOORLAND_PRISONER.number,
      type = "O",
      contactId = 100L,
      prisonerContactId = 201L,
    ).toPrisonerContactModel()

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(5L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true)).thenReturn(listOf(returnedContact))

    assertThrows<ValidationException> {
      service.updateVisitors(5L, MOORLAND, request, MOORLAND_PRISON_USER)
    }
    verify(officialVisitorRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `updateVisitors accepts visitor when both contactId and prisonerContactId match`() {
    val visit = createVisit(officialVisitId = 6L)
    val request = OfficialVisitUpdateVisitorsRequest(
      officialVisitors = listOf(
        OfficialVisitor(
          officialVisitorId = 0L,
          visitorTypeCode = VisitorType.CONTACT,
          contactId = 7L,
          prisonerContactId = 700L,
          relationshipCode = "POM",
        ),
      ),
    )

    val returnedContact = prisonerContact(
      prisonerNumber = MOORLAND_PRISONER.number,
      type = "O",
      contactId = 7L,
      prisonerContactId = 700L,
      firstName = "Jane",
      lastName = "Doe",
    ).toPrisonerContactModel()

    whenever(officialVisitRepository.findByOfficialVisitIdAndPrisonCode(6L, MOORLAND)).thenReturn(visit)
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, null, true)).thenReturn(listOf(returnedContact))
    whenever(officialVisitorRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] as OfficialVisitorEntity }

    val savedCaptor = argumentCaptor<OfficialVisitorEntity>()
    val response = service.updateVisitors(6L, MOORLAND, request, MOORLAND_PRISON_USER)

    verify(officialVisitorRepository).saveAndFlush(savedCaptor.capture())
    assertThat(response.visitorsAdded).hasSize(1)
    assertThat(savedCaptor.firstValue.firstName).isEqualTo("Jane")
    assertThat(savedCaptor.firstValue.prisonerContactId).isEqualTo(700L)
  }

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

  private fun addExistingVisitor(
    visit: OfficialVisitEntity,
    officialVisitorId: Long,
    contactId: Long,
    prisonerContactId: Long,
    firstName: String,
    lastName: String,
    relationshipCode: String,
    leadVisitor: Boolean = false,
    assistedVisit: Boolean = false,
    assistedNotes: String? = null,
    equipmentDescription: String? = null,
  ): OfficialVisitorEntity {
    val visitor = OfficialVisitorEntity(
      officialVisitorId = officialVisitorId,
      officialVisit = visit,
      visitorTypeCode = VisitorType.CONTACT,
      firstName = firstName,
      lastName = lastName,
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      relationshipTypeCode = RelationshipType.OFFICIAL,
      relationshipCode = relationshipCode,
      leadVisitor = leadVisitor,
      assistedVisit = assistedVisit,
      visitorNotes = assistedNotes,
      createdBy = "test",
      createdTime = LocalDateTime.now().minusHours(1),
    )

    if (!equipmentDescription.isNullOrBlank()) {
      visitor.visitorEquipment = VisitorEquipmentEntity(
        officialVisitor = visitor,
        description = equipmentDescription,
        createdBy = "test",
      )
    }

    visit.mutableVisitors().add(visitor)
    return visitor
  }

  @Suppress("UNCHECKED_CAST")
  private fun OfficialVisitEntity.mutableVisitors(): MutableList<OfficialVisitorEntity> {
    val field = OfficialVisitEntity::class.java.getDeclaredField("officialVisitors")
    field.isAccessible = true
    return field.get(this) as MutableList<OfficialVisitorEntity>
  }
}
