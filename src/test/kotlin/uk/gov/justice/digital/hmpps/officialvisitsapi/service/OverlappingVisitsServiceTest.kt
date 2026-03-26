package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OverlappingVisitsCriteriaRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OverlappingContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import java.time.LocalTime
import java.util.UUID
import kotlin.collections.listOf

class OverlappingVisitsServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val officialVisitorRepository: OfficialVisitorRepository = mock()
  private val overlappingVisitsService = OverlappingVisitsService(officialVisitRepository, officialVisitorRepository)

  @Test
  fun `should fail if request for visits in the past`() {
    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = today().minusDays(1),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      contactIds = listOf(1, 2),
    )

    val exception = assertThrows<IllegalArgumentException> {
      overlappingVisitsService.findOverlappingScheduledVisits(MOORLAND, request)
    }

    exception.message isEqualTo "Cannot overlap visits in the past. Visit date: ${request.visitDate} and start time: ${request.startTime} must be in the future."

    verifyNoInteractions(officialVisitRepository, officialVisitorRepository)
  }

  @Test
  fun `should be no clashes for future visit`() {
    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = tomorrow(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      contactIds = listOf(1, 2),
    )

    val response = overlappingVisitsService.findOverlappingScheduledVisits(MOORLAND, request)

    response.prisonerNumber isEqualTo MOORLAND_PRISONER.number
    response.overlappingPrisonerVisits hasSize 0
    response.contacts containsExactlyInAnyOrder listOf(
      OverlappingContact(1, emptyList()),
      OverlappingContact(2, emptyList()),
    )

    verify(officialVisitRepository).findScheduledOverlappingVisitsBy(
      MOORLAND,
      MOORLAND_PRISONER.number,
      tomorrow(),
      LocalTime.of(10, 0),
      LocalTime.of(11, 0),
    )
  }

  @Test
  fun `should be clashes for prisoner but not contacts`() {
    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = tomorrow(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      contactIds = listOf(1, 2),
    )

    whenever {
      officialVisitRepository.findScheduledOverlappingVisitsBy(
        MOORLAND,
        MOORLAND_PRISONER.number,
        tomorrow(),
        LocalTime.of(10, 0),
        LocalTime.of(11, 0),
      )
    } doReturn listOf(visit(1), visit(2))

    with(overlappingVisitsService.findOverlappingScheduledVisits(MOORLAND, request)) {
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      overlappingPrisonerVisits containsExactlyInAnyOrder listOf(1, 2)
      contacts containsExactlyInAnyOrder listOf(OverlappingContact(1, emptyList()), OverlappingContact(2, emptyList()))
    }
  }

  @Test
  fun `should be clashes for contact but not prisoner`() {
    val visit = visit(1)
    val visitor = visit.officialVisitors().single()

    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = tomorrow(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      contactIds = listOf(visitor.contactId!!),
    )

    whenever {
      officialVisitorRepository.findScheduledOverlappingVisitsBy(
        contactId = visitor.contactId!!,
        visitDate = tomorrow(),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
      )
    } doReturn listOf(visitor)

    with(overlappingVisitsService.findOverlappingScheduledVisits(MOORLAND, request)) {
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      overlappingPrisonerVisits hasSize 0
      contacts containsExactlyInAnyOrder listOf(
        OverlappingContact(visitor.contactId!!, listOf(visit.officialVisitId)),
      )
    }
  }

  @Test
  fun `should be clashes for prisoner and contact`() {
    val visit = visit(1)
    val visitor = visit.officialVisitors().single()

    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = tomorrow(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      contactIds = listOf(visitor.contactId!!),
    )

    whenever {
      officialVisitRepository.findScheduledOverlappingVisitsBy(
        MOORLAND,
        MOORLAND_PRISONER.number,
        tomorrow(),
        LocalTime.of(10, 0),
        LocalTime.of(11, 0),
      )
    } doReturn listOf(visit)

    whenever {
      officialVisitorRepository.findScheduledOverlappingVisitsBy(
        contactId = visitor.contactId!!,
        visitDate = tomorrow(),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
      )
    } doReturn listOf(visitor)

    with(overlappingVisitsService.findOverlappingScheduledVisits(MOORLAND, request)) {
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      overlappingPrisonerVisits containsExactly listOf(visit.officialVisitId)
      contacts containsExactlyInAnyOrder listOf(
        OverlappingContact(visitor.contactId!!, listOf(visit.officialVisitId)),
      )
    }
  }

  @Test
  fun `should be no clashes for prisoner and contact when visit excluded`() {
    val visit = visit(1)
    val visitor = visit.officialVisitors().single()

    val request = OverlappingVisitsCriteriaRequest(
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = tomorrow(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      contactIds = listOf(visitor.contactId!!),
      existingOfficialVisitId = visit.officialVisitId,
    )

    whenever {
      officialVisitRepository.findScheduledOverlappingVisitsBy(
        MOORLAND,
        MOORLAND_PRISONER.number,
        tomorrow(),
        LocalTime.of(10, 0),
        LocalTime.of(11, 0),
      )
    } doReturn listOf(visit)

    whenever {
      officialVisitorRepository.findScheduledOverlappingVisitsBy(
        contactId = visitor.contactId!!,
        visitDate = tomorrow(),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
      )
    } doReturn listOf(visitor)

    with(overlappingVisitsService.findOverlappingScheduledVisits(MOORLAND, request)) {
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      overlappingPrisonerVisits hasSize 0
      contacts containsExactlyInAnyOrder listOf(
        OverlappingContact(visitor.contactId!!, emptyList()),
      )
    }
  }

  private fun visit(officialVisitId: Long = 1L) = run {
    val prisonVisitSlot = PrisonVisitSlotEntity(
      prisonVisitSlotId = 1,
      prisonTimeSlotId = 1,
      dpsLocationId = UUID.randomUUID(),
      maxAdults = 1,
      maxGroups = 1,
      maxVideoSessions = 1,
      createdBy = "test-helper",
      createdTime = now(),
    )

    val visit = OfficialVisitEntity(
      officialVisitId = officialVisitId,
      prisonVisitSlot = prisonVisitSlot,
      prisonCode = PENTONVILLE,
      prisonerNumber = PENTONVILLE_PRISONER.number,
      visitDate = tomorrow(),
      startTime = LocalTime.of(11, 45),
      endTime = LocalTime.of(12, 45),
      dpsLocationId = prisonVisitSlot.dpsLocationId,
      visitTypeCode = VisitType.IN_PERSON,
      createdBy = "test-helper",
    )

    visit.addVisitor(
      visitorTypeCode = VisitorType.CONTACT,
      relationshipTypeCode = RelationshipType.OFFICIAL,
      relationshipCode = "COM",
      contactId = 1,
      prisonerContactId = 1,
      firstName = "Community",
      lastName = "Manager",
      leadVisitor = true,
      assistedVisit = true,
      assistedNotes = "assisted notes",
      createdBy = PENTONVILLE_PRISON_USER,
      createdTime = now(),
    )

    visit
  }
}
