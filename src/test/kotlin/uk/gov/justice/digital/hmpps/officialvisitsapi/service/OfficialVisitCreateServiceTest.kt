package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.slotavailability.AvailableSlotService
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class OfficialVisitCreateServiceTest {
  private val prisonerValidator: PrisonerValidator = mock()
  private val availableSlotService: AvailableSlotService = mock()
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository = mock()
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val contactsService: ContactsService = mock()
  private val auditingService: AuditingService = mock()
  private val metricsService: MetricsService = mock()

  private val service = OfficialVisitCreateService(
    prisonerValidator = prisonerValidator,
    availableSlotService = availableSlotService,
    prisonVisitSlotRepository = prisonVisitSlotRepository,
    officialVisitRepository = officialVisitRepository,
    prisonerVisitedRepository = prisonerVisitedRepository,
    contactsService = contactsService,
    auditingService = auditingService,
    metricsService = metricsService,
  )

  @Test
  fun `should fail if an identical visit from the same user was created within the last 15 minutes`() {
    val request = CreateOfficialVisitRequest(
      prisonVisitSlotId = 1L,
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = LocalDate.now().plusDays(1),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(10, 0),
      dpsLocationId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      visitTypeCode = VisitType.IN_PERSON,
      staffNotes = "private notes",
      prisonerNotes = "public notes",
      officialVisitors = listOf(
        OfficialVisitor(
          visitorTypeCode = VisitorType.CONTACT,
          contactId = 123L,
          prisonerContactId = 456L,
          relationshipCode = "POM",
          leadVisitor = true,
        ),
      ),
    )

    whenever(prisonVisitSlotRepository.findById(1L)).thenReturn(
      Optional.of(
        PrisonVisitSlotEntity(
          prisonVisitSlotId = 1L,
          prisonTimeSlotId = 1L,
          dpsLocationId = request.dpsLocationId,
          maxAdults = 10,
          maxGroups = 10,
          maxVideoSessions = 10,
          createdBy = MOORLAND_PRISON_USER.username,
          createdTime = java.time.LocalDateTime.now(),
        ),
      ),
    )
    whenever(prisonerValidator.validatePrisonerAtPrison(MOORLAND_PRISONER.number, MOORLAND)).thenReturn(
      prisonerSearchPrisoner(
        prisonerNumber = MOORLAND_PRISONER.number,
        prisonCode = MOORLAND,
        bookingId = MOORLAND_PRISONER.bookingId,
      ),
    )
    whenever(availableSlotService.getAvailableSlotsForPrison(MOORLAND, request.visitDate, request.visitDate, false)).thenReturn(
      listOf(
        AvailableSlot(
          visitSlotId = 1L,
          timeSlotId = 1L,
          prisonCode = MOORLAND,
          dayCode = "MON",
          dayDescription = "Monday",
          visitDate = request.visitDate,
          startTime = request.startTime,
          endTime = request.endTime,
          dpsLocationId = request.dpsLocationId,
          availableVideoSessions = 2,
          availableAdults = 10,
          availableGroups = 10,
        ),
      ),
    )
    whenever(contactsService.getAllPrisonerContacts(MOORLAND_PRISONER.number, true, true)).thenReturn(
      listOf(
        PrisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          lastName = "Doe",
          firstName = "John",
          relationshipTypeCode = "O",
          relationshipTypeDescription = "Official",
          relationshipToPrisonerCode = "POM",
          relationshipToPrisonerDescription = "Prison Offender Manager",
          isApprovedVisitor = true,
          isNextOfKin = false,
          isEmergencyContact = false,
          isRelationshipActive = true,
          currentTerm = true,
          isStaff = false,
          restrictionSummary = mock(),
          prisonerContactId = 456L,
          contactId = 123L,
        ),
      ),
    )
    whenever(officialVisitRepository.findRecentDuplicateRequest(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(mock())

    val exception = assertThrows<RuntimeException> {
      service.create(MOORLAND, request, MOORLAND_PRISON_USER)
    }

    assertThat(exception.message).isEqualTo(
      "Prisoner ${MOORLAND_PRISONER.number} already has an official visit booked at $MOORLAND on ${request.visitDate} at ${request.startTime} for the same staff user ${MOORLAND_PRISON_USER.username} within the last 15 minutes.",
    )
  }
}
