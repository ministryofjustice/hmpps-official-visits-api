package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.job

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alertsapi.model.Alert
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alertsapi.model.AlertCodeSummary
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class JobTriggerIntegrationTest : IntegrationTestBase() {

  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 123,
    prisonerContactId = 456,
    leadVisitor = true,
  )

  @BeforeEach
  fun setupTest() {
    clearAllVisitData()
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
    alertsApi().stubGetPrisonerAlertsNotFound(MOORLAND_PRISONER.number)
  }

  @AfterEach
  fun tearDown() {
    clearAllVisitData()
  }

  @Nested
  inner class IdentifyCandidateVisitsToCheckJobTest {

    @Test
    fun `should identify candidate visits to check`() {
      val sixDaysInFuture = LocalDate.now().plusDays(6)
      val (timeSlot, visitSlot) = testAPIClient.generateVisitSlot(sixDaysInFuture)

      testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot.visitSlotId, sixDaysInFuture, timeSlot.startTime, timeSlot.endTime, moorlandLocation.id), listOf(officialVisitor)),
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
        createOfficialVisitRequest(MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
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
  inner class IdentifyCandidateVisitsToReCheckJobTest {

    @Test
    fun `should identify visits to recheck when it is scheduled for day after tomorrow`() {
      val twoDaysInFuture = LocalDate.now().plusDays(2)
      val (timeSlot, visitSlot) = testAPIClient.generateVisitSlot(twoDaysInFuture)

      testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot.visitSlotId, twoDaysInFuture, timeSlot.startTime, timeSlot.endTime, moorlandLocation.id), listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_RECHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1
    }

    @Test
    fun `should not identify visits to recheck for visits that are less than 2 days or more than 2 day to the future`() {
      val threeDaysInFuture = LocalDate.now().plusDays(3)
      val (timeSlot, visitSlot) = testAPIClient.generateVisitSlot(threeDaysInFuture)

      testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot.visitSlotId, threeDaysInFuture, timeSlot.startTime, timeSlot.endTime, moorlandLocation.id), listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      val oneDaysInFuture = LocalDate.now().plusDays(1)
      val (timeSlot2, visitSlot2) = testAPIClient.generateVisitSlot(oneDaysInFuture)

      testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot2.visitSlotId, oneDaysInFuture, timeSlot2.startTime, timeSlot2.endTime, moorlandLocation.id), listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_RECHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0
    }

    @Test
    fun `should identify visits to recheck when visit is not cancelled or completed`() {
      val twoDaysInFuture = LocalDate.now().plusDays(2)
      val (timeSlot1, visitSlot1) = testAPIClient.generateVisitSlot(twoDaysInFuture, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))
      val cancelledVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot1.visitSlotId, twoDaysInFuture, timeSlot1.startTime, timeSlot1.endTime, moorlandLocation.id), listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )
      officialVisitRepository.findById(cancelledVisit.officialVisitId).orElseThrow().apply {
        visitStatusCode = VisitStatusType.CANCELLED
        officialVisitRepository.saveAndFlush(this)
      }

      val (timeSlot2, visitSlot2) = testAPIClient.generateVisitSlot(twoDaysInFuture)

      val completedVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot2.visitSlotId, twoDaysInFuture, timeSlot2.startTime, timeSlot2.endTime, moorlandLocation.id), listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )
      officialVisitRepository.findById(completedVisit.officialVisitId).orElseThrow().apply {
        visitStatusCode = VisitStatusType.COMPLETED
        officialVisitRepository.saveAndFlush(this)
      }

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_RECHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0
    }
  }

  @Nested
  inner class ProcessCandidateVisitsToCheckJobTest {

    @Test
    fun `should process visits and not flag reviews when there are no issues found`() {
      val visit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visit.officialVisitId,
          createdTime = LocalDateTime.now(),
          triggeringEvent = VisitReviewCheckType.CHECK,
        ),
      )

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0
      val reviews = testAPIClient.getVisitsForReviewList()
      with(reviews) {
        content isEqualTo emptyList()
      }
    }

    @Test
    fun `should process visits and flag reviews when there are new active prisoner alerts found`() {
      val visit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visit.officialVisitId,
          createdTime = LocalDateTime.now(),
          triggeringEvent = VisitReviewCheckType.CHECK,
        ),
      )
      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(alert(true, LocalDateTime.now())))

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0
      val reviews = testAPIClient.getVisitsForReviewList()
      with(reviews) {
        val response = content[0]
        response.issues.size isEqualTo 1
        response.issues[0].issueType isEqualTo IssueType.PRISONER_NEW_ALERT
      }
    }

    @Test
    fun `should process visits and flag contact issues reviews when there are contact issues found`() {
      val visit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visit.officialVisitId,
          createdTime = LocalDateTime.now(),
          triggeringEvent = VisitReviewCheckType.CHECK,
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

      visitReviewQueueRepository.findAll().size isEqualTo 0
      val visitAfterFirstCheck = testAPIClient.getVisitsForReviewList()
      with(visitAfterFirstCheck) {
        val response = content[0]
        response.issues.size isEqualTo 1
        response.issues[0].issueType isEqualTo IssueType.VISITOR_NOT_OFFICIAL
      }
    }

    @Test
    fun `should process visits and flag issues when there are prisoner issues found`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val visit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visit.officialVisitId,
          createdTime = LocalDateTime.now(),
          triggeringEvent = VisitReviewCheckType.CHECK,
        ),
      )

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0
      val reviews = testAPIClient.getVisitsForReviewList()
      with(reviews) {
        val response = content[0]
        response.issues.size isEqualTo 1
        response.issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      }
    }

    @Test
    fun `should process visits and do not flag issues for visits that are more than 7 day to the future`() {
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
          triggeringEvent = VisitReviewCheckType.CHECK,
        ),
      )

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0
      val visitAfterFirstCheck = testAPIClient.getVisitsForReviewList()
      with(visitAfterFirstCheck) {
        content isEqualTo emptyList()
      }
    }

    @Test
    fun `should identify and flag issues for multiple visits scheduled for day after tomorrow`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val twoDaysInFuture = LocalDate.now().plusDays(2)
      val (timeSlot1, visitSlot1) = testAPIClient.generateVisitSlot(twoDaysInFuture, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))
      testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot1.visitSlotId, twoDaysInFuture, timeSlot1.startTime, timeSlot1.endTime, moorlandLocation.id), listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      val (timeSlot2, visitSlot2) = testAPIClient.generateVisitSlot(twoDaysInFuture)

      testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot2.visitSlotId, twoDaysInFuture, timeSlot2.startTime, timeSlot2.endTime, moorlandLocation.id), listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_RECHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 2

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0

      val visitAfterFirstCheck = testAPIClient.getVisitsForReviewList()
      with(visitAfterFirstCheck) {
        content[0].issues.size isEqualTo 1
        content[0].issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
        content[1].issues.size isEqualTo 1
        content[1].issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      }
    }

    @Test
    fun `should identify and flag issues for visits with previous acknowledged issues`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val twoDaysInFuture = LocalDate.now().plusDays(2)
      val (timeSlot1, visitSlot1) = testAPIClient.generateVisitSlot(twoDaysInFuture, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))
      val matchingVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot1.visitSlotId, twoDaysInFuture, timeSlot1.startTime, timeSlot1.endTime, moorlandLocation.id), listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_RECHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0
      val visitAfterFirstCheck = testAPIClient.getVisitsForReviewList()
      with(visitAfterFirstCheck) {
        val response = content[0]
        response.issues.size isEqualTo 1
        response.issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      }

      testAPIClient.acknowledgeVisitForReview(matchingVisit.officialVisitId, MOORLAND_PRISON_USER)

      val visitAfterAcknowledgement = testAPIClient.getVisitsForReviewList()
      with(visitAfterAcknowledgement) {
        content isEqualTo emptyList()
      }

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_RECHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1

      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(alert(true, LocalDateTime.now())))

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      val visitAfterLatestCheck = testAPIClient.getVisitsForReviewList()
      with(visitAfterLatestCheck) {
        val response = content[0]
        response.issues.size isEqualTo 2
        response.issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
        response.issues[1].issueType isEqualTo IssueType.PRISONER_NEW_ALERT
      }
    }

    @Test
    fun `should identify and flag issues for visits with previously unacknowledged issues with new issues`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val twoDaysInFuture = LocalDate.now().plusDays(2)
      val (timeSlot1, visitSlot1) = testAPIClient.generateVisitSlot(twoDaysInFuture, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))
      val matchingVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(VisitSlot(visitSlot1.visitSlotId, twoDaysInFuture, timeSlot1.startTime, timeSlot1.endTime, moorlandLocation.id), listOf(officialVisitor)),
        MOORLAND_PRISON_USER,
      )

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_RECHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 0
      val visitAfterFirstCheck = testAPIClient.getVisitsForReviewList()
      with(visitAfterFirstCheck) {
        val response = content[0]
        response.issues.size isEqualTo 1
        response.issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      }

      testAPIClient.runJob("IDENTIFY_CANDIDATE_VISITS_TO_RECHECK")

      visitReviewQueueRepository.findAll().size isEqualTo 1

      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(alert(true, LocalDateTime.now())))

      testAPIClient.runJob("PROCESS_CANDIDATE_VISITS_TO_CHECK")

      val visitAfterLatestCheck = testAPIClient.getVisitsForReviewList()
      with(visitAfterLatestCheck) {
        val response = content[0]
        response.issues.size isEqualTo 2
        response.issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
        response.issues[1].issueType isEqualTo IssueType.PRISONER_NEW_ALERT
      }
    }

    @Test
    fun `should not process when no candidate for visits to check`() {
      val matchingVisit = testAPIClient.createOfficialVisit(
        createOfficialVisitRequest(MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
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
      val reviews = testAPIClient.getVisitsForReviewList()
      with(reviews) {
        val response = content[0]
        response.issues.size isEqualTo 2
        response.issues[0].issueType isEqualTo IssueType.VISITOR_NOT_APPROVED
        response.issues[1].issueType isEqualTo IssueType.PRISONER_TRANSFERRED
      }
    }

    private fun alert(isActive: Boolean, createdAt: LocalDateTime): Alert = Alert(
      alertUuid = UUID.randomUUID(),
      prisonNumber = "A1234BC",
      alertCode = AlertCodeSummary(
        alertTypeCode = "X",
        alertTypeDescription = "Test Alert",
        code = "X1",
        description = "Test Alert Description",
        canBeAdministered = true,
      ),
      activeFrom = LocalDate.now(),
      isActive = isActive,
      createdAt = createdAt,
      createdBy = "test-user",
      createdByDisplayName = "Test User",
    )
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
}
