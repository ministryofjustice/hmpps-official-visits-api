package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.job

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER_ADDED
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER_INACTIVE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland.MONDAY_9_TO_10_VISIT_SLOT
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.VisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation2
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class JobTriggerIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var visitReviewRepository: VisitReviewRepository

  @Autowired
  private lateinit var visitReviewQueueRepository: VisitReviewQueueRepository

  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 123,
    prisonerContactId = 456,
    leadVisitor = true,
  )

  @BeforeEach
  fun setupTest() {
    clearStagedVisitData()
    prisonerSearchApi().stubGetPrisonName(MOORLAND, MOORLAND_PRISONER)
    locationsInsidePrisonApi().stubGetLocationById(moorlandLocation)
    locationsInsidePrisonApi().stubGetLocationById(moorlandLocation2)
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(MOORLAND, listOf(moorlandLocation, moorlandLocation2))
    personalRelationshipsApi().stubReferenceGroup()
    personalRelationshipsApi().stubForContactById(
      prisonerContact(
        prisonerNumber = MOORLAND_PRISONER.number,
        type = "O",
        contactId = 123,
        prisonerContactId = 456,
      ),
    )
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 123,
          prisonerContactId = 456,
        ),
      ),
    )
  }

  @AfterEach
  fun tearDown() {
    clearStagedVisitData()
  }

  @Nested
  inner class IdentifyCandidateVisitsToCheckJobTest {

    @Test
    fun `should find the identify candidate visits to check`() {
      testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1
    }

    @Test
    fun `should not find the identify candidate visits to check for visits that are more than 7 day to the future`() {
      val visitSlot = VisitSlot(
        1,
        LocalDate.now().next(DayOfWeek.MONDAY).plusDays(7),
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
        moorlandLocation.id,
      )
      val matchingVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(visitSlot, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0
    }

    @Test
    fun `should not find identify candidate visits to check`() {
      val matchingVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )
      val cancelledVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(Moorland.WEDNESDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )
      officialVisitRepository.findById(cancelledVisit.officialVisitId).orElseThrow().apply {
        visitStatusCode = VisitStatusType.CANCELLED
        officialVisitRepository.saveAndFlush(this)
      }

      createVisitReview(
        officialVisitId = matchingVisit.officialVisitId,
        issueTypes = listOf(IssueType.VISITOR_NOT_APPROVED, IssueType.PRISONER_TRANSFERRED),
      )
      createVisitReview(cancelledVisit.officialVisitId)

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_CHECK")
      visitReviewQueueRepository.findAll().size isEqualTo 0
    }
  }

  @Nested
  inner class ProcessCandidateVisitsToCheckJobTest {

    @Test
    fun `should processing visit review be completed with no reviews when there are no issues found`() {
      val visit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visit.officialVisitId,
          createdTime = LocalDateTime.now(),
          triggeringEvent = "CHECK",
        ),
      )

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1
      visitReviewRepository.findAll().size isEqualTo 0
    }

    @Test
    fun `should processing visit review be completed with contact issues reviews when there are issues found`() {
      val visit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visit.officialVisitId,
          createdTime = LocalDateTime.now(),
          triggeringEvent = "CHECK",
        ),
      )

      personalRelationshipsApi().stubAllContacts(
        prisonerNumber = MOORLAND_PRISONER.number,
        prisonerContacts = listOf(
          prisonerContact(
            prisonerNumber = MOORLAND_PRISONER.number,
            type = "S",
            contactId = CONTACT_MOORLAND_PRISONER.contactId,
            prisonerContactId = CONTACT_MOORLAND_PRISONER.prisonerContactId,
          ),
          prisonerContact(
            prisonerNumber = MOORLAND_PRISONER.number,
            type = "O",
            contactId = CONTACT_MOORLAND_PRISONER_ADDED.contactId,
            prisonerContactId = CONTACT_MOORLAND_PRISONER_ADDED.prisonerContactId,
          ),
        ),
      )

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1
      visitReviewRepository.findAll().size isEqualTo 1
    }

    @Test
    fun `should process visits when there are issues found`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val visit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visit.officialVisitId,
          createdTime = LocalDateTime.now(),
          triggeringEvent = "CHECK",
        ),
      )

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1
      visitReviewRepository.findAll().size isEqualTo 1
    }

    @Test
    fun `should not process candidates for visits that are more than 7 day to the future`() {
      val visitSlot = VisitSlot(
        1,
        LocalDate.now().next(DayOfWeek.MONDAY).plusDays(7),
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
        moorlandLocation.id,
      )
      val visit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(visitSlot, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visit.officialVisitId,
          createdTime = LocalDateTime.now().minusDays(7),
          triggeringEvent = "CHECK",
        ),
      )

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1
      visitReviewRepository.findAll().size isEqualTo 0
    }

    @Test
    fun `should not process when no candidate for visits to check`() {
      val matchingVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )
      val cancelledVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(Moorland.WEDNESDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )
      officialVisitRepository.findById(cancelledVisit.officialVisitId).orElseThrow().apply {
        visitStatusCode = VisitStatusType.CANCELLED
        officialVisitRepository.saveAndFlush(this)
      }

      createVisitReview(
        officialVisitId = matchingVisit.officialVisitId,
        issueTypes = listOf(IssueType.VISITOR_NOT_APPROVED, IssueType.PRISONER_TRANSFERRED),
      )
      createVisitReview(cancelledVisit.officialVisitId)

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")
      visitReviewQueueRepository.findAll().size isEqualTo 0
      visitReviewRepository.findAll().size isEqualTo 2
    }
  }

  @Nested
  inner class VisitsReviewExpireJobTest {
    @Test
    fun `should expire visits for review when the visit date is in the past`() {
      val visit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      val pastVisit = officialVisitRepository.findById(visit.officialVisitId).orElseThrow().apply { visitDate = LocalDate.now().minusDays(20) }
      officialVisitRepository.saveAndFlush(pastVisit)

      createVisitReview(
        officialVisitId = visit.officialVisitId,
        issueTypes = listOf(IssueType.VISITOR_NOT_APPROVED, IssueType.PRISONER_TRANSFERRED),
      )

      testAPIClient.runJob("EXPIRE_VISITS_FOR_REVIEW")
      val visitReview = visitReviewRepository.findByOfficialVisitId(visit.officialVisitId)
      visitReview[0].expiredTime isCloseTo LocalDateTime.now()
    }
  }

  private fun createVisitReview(
    officialVisitId: Long,
    expiredTime: LocalDateTime? = null,
    issueTypes: List<IssueType> = listOf(IssueType.VISITOR_NOT_APPROVED),
  ): VisitReviewEntity {
    val review = VisitReviewEntity(
      officialVisitId = officialVisitId,
      raisedTime = LocalDateTime.now(),
    )
    review.expiredTime = expiredTime
    issueTypes.forEach { issueType ->
      review.addVisitReviewDetails(LocalDateTime.now(), issueType, null)
    }

    return visitReviewRepository.saveAndFlush(review)
  }

  private fun clearStagedVisitData() {
    visitReviewRepository.deleteAll()
    visitReviewQueueRepository.deleteAll()
    clearAllVisitData()
  }
}
