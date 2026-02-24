package uk.gov.justice.digital.hmpps.officialvisitsapi.service.slotavailability

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isBool
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot
import java.time.LocalTime
import java.util.UUID

class AvailableSlotSpecificationFactoryTest {
  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 123,
    prisonerContactId = 456,
    leadVisitor = true,
    assistedVisit = false,
    assistedNotes = "visitor notes",
    visitorEquipment = VisitorEquipment("Bringing secure laptop"),
    officialVisitorId = 0L,
  )

  private val nextMondayAt9 = CreateOfficialVisitRequest(
    prisonerNumber = MOORLAND_PRISONER.number,
    prisonVisitSlotId = 1,
    visitDate = tomorrow(),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    visitTypeCode = VisitType.IN_PERSON,
    staffNotes = "private notes",
    prisonerNotes = "public notes",
    searchTypeCode = SearchLevelType.PAT,
    officialVisitors = listOf(officialVisitor),
  )

  private val prisonVisitSlot: PrisonVisitSlotEntity = mock()

  @BeforeEach
  fun before() {
    prisonVisitSlot.stub { on { prisonVisitSlotId } doReturn 1 }
  }

  @Nested
  inner class InPersonVisit {
    private val inPersonVisit = nextMondayAt9.copy(visitTypeCode = VisitType.IN_PERSON)

    @Test
    fun `should return in person specification`() {
      AvailableSlotSpecificationFactory.getAvailableSlotSpecification(inPersonVisit) isInstanceOf InPersonSpecification::class.java
    }

    @Test
    fun `should be available`() {
      val specification = AvailableSlotSpecificationFactory.getAvailableSlotSpecification(inPersonVisit)

      val availableSlot = availableSlot(availableVideoSessions = 1, availableAdults = 1, availableGroups = 1)

      specification.isSatisfiedBy(availableSlot, prisonVisitSlot) isBool true
    }

    @Test
    fun `should not be available when exceeds available groups`() {
      val specification = AvailableSlotSpecificationFactory.getAvailableSlotSpecification(inPersonVisit)

      val availableSlot = availableSlot(availableVideoSessions = 0, availableAdults = 1, availableGroups = 0)

      specification.isSatisfiedBy(availableSlot, prisonVisitSlot) isBool false
    }

    @Test
    fun `should not be available when exceeds available adults`() {
      val specification = AvailableSlotSpecificationFactory.getAvailableSlotSpecification(inPersonVisit)

      val availableSlot = availableSlot(availableVideoSessions = 1, availableAdults = 0, availableGroups = 1)

      specification.isSatisfiedBy(availableSlot, prisonVisitSlot) isBool false
    }
  }

  @Nested
  inner class VideoVisit {
    private val videoVisit = nextMondayAt9.copy(visitTypeCode = VisitType.VIDEO)

    @Test
    fun `should return video specification`() {
      AvailableSlotSpecificationFactory.getAvailableSlotSpecification(videoVisit) isInstanceOf VideoSpecification::class.java
    }

    @Test
    fun `should be available`() {
      val specification = AvailableSlotSpecificationFactory.getAvailableSlotSpecification(videoVisit)

      val availableSlot = availableSlot(availableVideoSessions = 1, availableAdults = 0, availableGroups = 0)

      specification.isSatisfiedBy(availableSlot, prisonVisitSlot) isBool true
    }

    @Test
    fun `should not be available when exceeds available video sessions`() {
      val specification = AvailableSlotSpecificationFactory.getAvailableSlotSpecification(videoVisit)

      val availableSlot = availableSlot(availableVideoSessions = 0, availableAdults = 1, availableGroups = 1)

      specification.isSatisfiedBy(availableSlot, prisonVisitSlot) isBool false
    }
  }

  @Nested
  inner class TelephoneVisit {
    private val telephoneVisit = nextMondayAt9.copy(visitTypeCode = VisitType.TELEPHONE)

    @Test
    fun `should return telephone specification`() {
      AvailableSlotSpecificationFactory.getAvailableSlotSpecification(telephoneVisit) isInstanceOf TelephoneSpecification::class.java
    }

    @Test
    fun `should be available`() {
      val specification = AvailableSlotSpecificationFactory.getAvailableSlotSpecification(telephoneVisit)

      val availableSlot = availableSlot(availableVideoSessions = 1, availableAdults = 1, availableGroups = 1)

      specification.isSatisfiedBy(availableSlot, prisonVisitSlot) isBool true
    }

    @Test
    fun `should not be available when no available adults or groups `() {
      val specification = AvailableSlotSpecificationFactory.getAvailableSlotSpecification(telephoneVisit)

      val availableSlot = availableSlot(availableVideoSessions = 1, availableAdults = 0, availableGroups = 0)

      specification.isSatisfiedBy(availableSlot, prisonVisitSlot) isBool false
    }
  }

  private fun availableSlot(
    availableVideoSessions: Int,
    availableAdults: Int,
    availableGroups: Int,
  ): AvailableSlot = AvailableSlot(
    visitSlotId = 1,
    timeSlotId = 1,
    prisonCode = MOORLAND,
    dayCode = "MON",
    dayDescription = "Monday",
    visitDate = tomorrow(),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    availableVideoSessions = availableVideoSessions,
    availableAdults = availableAdults,
    availableGroups = availableGroups,
    locationDescription = "Test location description",
  )
}
