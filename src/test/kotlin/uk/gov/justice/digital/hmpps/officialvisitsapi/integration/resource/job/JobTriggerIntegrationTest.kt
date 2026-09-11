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

  private companion object {
    private const val JOB_IDENTIFY_CANDIDATE_VISITS_TO_CHECK = "IDENTIFY_CANDIDATE_VISITS_TO_CHECK"
    private const val JOB_IDENTIFY_CANDIDATE_VISITS_TO_RECHECK = "IDENTIFY_CANDIDATE_VISITS_TO_RECHECK"
    private const val JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK = "PROCESS_CANDIDATE_VISITS_TO_CHECK"
    private const val JOB_EXPIRE_VISITS_FOR_REVIEW = "EXPIRE_VISITS_FOR_REVIEW"

    private const val CHECK_WINDOW_SEVEN_DAYS = 7L

    private const val RECHECK_TARGET_DAYS = 2L
  }

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

  // ---------------------------------------------------------------------
  // Test data / fixture helpers
  // ---------------------------------------------------------------------

  /** Creates an official visit on a pre-built [VisitSlot] (e.g. one of the fixed [Moorland] slots). */
  private fun createVisitOnSlot(slot: VisitSlot, visitors: List<OfficialVisitor> = listOf(officialVisitor)) = testAPIClient.createOfficialVisit(createOfficialVisitRequest(slot, visitors), MOORLAND_PRISON_USER)

  /** Creates an official visit on [date], letting [testAPIClient] pick an available time slot. */
  private fun createVisitOnDate(date: LocalDate, visitors: List<OfficialVisitor> = listOf(officialVisitor)) = testAPIClient.generateVisitSlot(date).let { (timeSlot, visitSlot) ->
    createVisitOnSlot(VisitSlot(visitSlot.visitSlotId, date, timeSlot.startTime, timeSlot.endTime, moorlandLocation.id), visitors)
  }

  /** Creates an official visit on [date] at an explicit [startTime]/[endTime]. */
  private fun createVisitOnDateAndTimes(date: LocalDate, startTime: LocalTime, endTime: LocalTime, visitors: List<OfficialVisitor> = listOf(officialVisitor)) = testAPIClient.generateVisitSlot(date, startTime = startTime, endTime = endTime).let { (timeSlot, visitSlot) ->
    createVisitOnSlot(VisitSlot(visitSlot.visitSlotId, date, timeSlot.startTime, timeSlot.endTime, moorlandLocation.id), visitors)
  }

  /** A fixed slot that sits just outside the job's [CHECK_WINDOW_SEVEN_DAYS] pickup window. */
  private fun slotBeyondCheckWindow() = VisitSlot(
    1,
    LocalDate.now().next(DayOfWeek.MONDAY).plusDays(CHECK_WINDOW_SEVEN_DAYS),
    LocalTime.of(9, 0),
    LocalTime.of(10, 0),
    moorlandLocation.id,
  )

  private fun markVisitStatus(visitId: Long, status: VisitStatusType) {
    officialVisitRepository.findById(visitId).orElseThrow().apply {
      visitStatusCode = status
      officialVisitRepository.saveAndFlush(this)
    }
  }

  private fun setVisitDate(visitId: Long, date: LocalDate) {
    officialVisitRepository.findById(visitId).orElseThrow().apply {
      visitDate = date
      officialVisitRepository.saveAndFlush(this)
    }
  }

  /** Puts a visit straight onto the review queue, bypassing the identify job. */
  private fun enqueueForReview(
    visitId: Long,
    triggeringEvent: VisitReviewCheckType = VisitReviewCheckType.CHECK,
    createdTime: LocalDateTime = LocalDateTime.now(),
  ) {
    visitReviewQueueRepository.saveAndFlush(
      VisitReviewQueueEntity(
        officialVisitId = visitId,
        createdTime = createdTime,
        triggeringEvent = triggeringEvent,
      ),
    )
  }

  private fun queueSize() = visitReviewQueueRepository.findAll().size

  private fun assertQueueSize(expected: Int) {
    queueSize() isEqualTo expected
  }

  private fun reviewListContent() = testAPIClient.getVisitsForReviewList().content

  private fun firstReviewIssues() = reviewListContent()[0].issues

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

  @Nested
  inner class IdentifyCandidateVisitsToCheckJobTest {

    @Test
    fun `should identify candidate visits to check`() {
      createVisitOnDate(LocalDate.now().plusDays(6))

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_CHECK)

      assertQueueSize(1)
    }

    @Test
    fun `should not find the identify candidate visits to check for visits that are more than 7 day to the future`() {
      createVisitOnSlot(slotBeyondCheckWindow())

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_CHECK)

      assertQueueSize(0)
    }

    @Test
    fun `should not find identify candidate visits to check`() {
      val matchingVisit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      val cancelledVisit = createVisitOnSlot(Moorland.WEDNESDAY_9_TO_10_VISIT_SLOT)
      markVisitStatus(cancelledVisit.officialVisitId, VisitStatusType.CANCELLED)

      createVisitReview(
        officialVisitId = matchingVisit.officialVisitId,
        issueTypes = listOf(IssueType.VISITOR_NOT_APPROVED, IssueType.PRISONER_TRANSFERRED),
      )
      createVisitReview(cancelledVisit.officialVisitId)

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_CHECK)

      assertQueueSize(0)
    }
  }

  @Nested
  inner class IdentifyCandidateVisitsToReCheckJobTest {

    @Test
    fun `should identify visits to recheck when it is scheduled for day after tomorrow`() {
      createVisitOnDate(LocalDate.now().plusDays(RECHECK_TARGET_DAYS))

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_RECHECK)

      assertQueueSize(1)
    }

    @Test
    fun `should not identify visits to recheck for visits that are less than 2 days or more than 2 day to the future`() {
      createVisitOnDate(LocalDate.now().plusDays(RECHECK_TARGET_DAYS + 1))
      createVisitOnDate(LocalDate.now().plusDays(RECHECK_TARGET_DAYS - 1))

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_RECHECK)

      assertQueueSize(0)
    }

    @Test
    fun `should identify visits to recheck when visit is not cancelled or completed`() {
      val twoDaysInFuture = LocalDate.now().plusDays(RECHECK_TARGET_DAYS)

      val cancelledVisit = createVisitOnDateAndTimes(twoDaysInFuture, LocalTime.of(9, 0), LocalTime.of(10, 0))
      markVisitStatus(cancelledVisit.officialVisitId, VisitStatusType.CANCELLED)

      val completedVisit = createVisitOnDate(twoDaysInFuture)
      markVisitStatus(completedVisit.officialVisitId, VisitStatusType.COMPLETED)

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_RECHECK)

      assertQueueSize(0)
    }
  }

  @Nested
  inner class ProcessCandidateVisitsToCheckJobTest {

    @Test
    fun `should process visits and not flag reviews when there are no issues found`() {
      val visit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      enqueueForReview(visit.officialVisitId)

      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)

      assertQueueSize(0)
      reviewListContent() isEqualTo emptyList()
    }

    @Test
    fun `should process visits and flag reviews when there are new active prisoner alerts found`() {
      val visit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      enqueueForReview(visit.officialVisitId)
      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(alert(true, LocalDateTime.now())))

      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)

      assertQueueSize(0)
      val issues = firstReviewIssues()
      issues.size isEqualTo 1
      issues[0].issueType isEqualTo IssueType.PRISONER_NEW_ALERT
    }

    @Test
    fun `should process visits and flag contact issues reviews when there are contact issues found`() {
      val visit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      enqueueForReview(visit.officialVisitId)

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

      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)

      assertQueueSize(0)
      val issues = firstReviewIssues()
      issues.size isEqualTo 1
      issues[0].issueType isEqualTo IssueType.VISITOR_NOT_OFFICIAL
    }

    @Test
    fun `should process visits and flag issues when there are prisoner issues found`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val visit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      enqueueForReview(visit.officialVisitId)

      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)

      assertQueueSize(0)
      val issues = firstReviewIssues()
      issues.size isEqualTo 1
      issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
    }

    @Test
    fun `should process visits and do not flag issues for visits that are more than 7 day to the future`() {
      val visit = createVisitOnSlot(slotBeyondCheckWindow())
      enqueueForReview(visit.officialVisitId, createdTime = LocalDateTime.now().minusDays(CHECK_WINDOW_SEVEN_DAYS))

      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)

      assertQueueSize(0)
      reviewListContent() isEqualTo emptyList()
    }

    @Test
    fun `should identify and flag issues for multiple visits scheduled for day after tomorrow`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val twoDaysInFuture = LocalDate.now().plusDays(RECHECK_TARGET_DAYS)

      createVisitOnDateAndTimes(twoDaysInFuture, LocalTime.of(9, 0), LocalTime.of(10, 0))
      createVisitOnDate(twoDaysInFuture)

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_RECHECK)
      assertQueueSize(2)

      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)
      assertQueueSize(0)

      val reviews = reviewListContent()
      reviews[0].issues.size isEqualTo 1
      reviews[0].issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      reviews[1].issues.size isEqualTo 1
      reviews[1].issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
    }

    @Test
    fun `should identify and flag issues for visits with previous acknowledged issues`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val twoDaysInFuture = LocalDate.now().plusDays(RECHECK_TARGET_DAYS)
      val matchingVisit = createVisitOnDateAndTimes(twoDaysInFuture, LocalTime.of(9, 0), LocalTime.of(10, 0))

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_RECHECK)
      assertQueueSize(1)

      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)
      assertQueueSize(0)
      firstReviewIssues().let { issues ->
        issues.size isEqualTo 1
        issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      }

      testAPIClient.acknowledgeVisitForReview(matchingVisit.officialVisitId, MOORLAND_PRISON_USER)
      reviewListContent() isEqualTo emptyList()

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_RECHECK)
      assertQueueSize(1)

      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(alert(true, LocalDateTime.now())))
      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)

      val issues = firstReviewIssues()
      issues.size isEqualTo 2
      issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      issues[1].issueType isEqualTo IssueType.PRISONER_NEW_ALERT
    }

    @Test
    fun `should identify and flag issues for visits with previously unacknowledged issues with new issues`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val twoDaysInFuture = LocalDate.now().plusDays(RECHECK_TARGET_DAYS)
      createVisitOnDateAndTimes(twoDaysInFuture, LocalTime.of(9, 0), LocalTime.of(10, 0))

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_RECHECK)
      assertQueueSize(1)

      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)
      assertQueueSize(0)
      firstReviewIssues().let { issues ->
        issues.size isEqualTo 1
        issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      }

      testAPIClient.runJob(JOB_IDENTIFY_CANDIDATE_VISITS_TO_RECHECK)
      assertQueueSize(1)

      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(alert(true, LocalDateTime.now())))
      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)

      val issues = firstReviewIssues()
      issues.size isEqualTo 2
      issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      issues[1].issueType isEqualTo IssueType.PRISONER_NEW_ALERT
    }

    @Test
    fun `should not process when no candidate for visits to check`() {
      val matchingVisit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      val cancelledVisit = createVisitOnSlot(Moorland.WEDNESDAY_9_TO_10_VISIT_SLOT)
      markVisitStatus(cancelledVisit.officialVisitId, VisitStatusType.CANCELLED)

      createVisitReview(
        officialVisitId = matchingVisit.officialVisitId,
        issueTypes = listOf(IssueType.VISITOR_NOT_APPROVED, IssueType.PRISONER_TRANSFERRED),
      )
      createVisitReview(cancelledVisit.officialVisitId)

      testAPIClient.runJob(JOB_PROCESS_CANDIDATE_VISITS_TO_CHECK)

      assertQueueSize(0)
      val issues = firstReviewIssues()
      issues.size isEqualTo 2
      issues[0].issueType isEqualTo IssueType.VISITOR_NOT_APPROVED
      issues[1].issueType isEqualTo IssueType.PRISONER_TRANSFERRED
    }
  }

  @Nested
  inner class VisitsReviewExpireJobTest {
    @Test
    fun `should expire visits for review when the visit date is in the past`() {
      val visit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      setVisitDate(visit.officialVisitId, LocalDate.now().minusDays(20))

      createVisitReview(
        officialVisitId = visit.officialVisitId,
        issueTypes = listOf(IssueType.VISITOR_NOT_APPROVED, IssueType.PRISONER_TRANSFERRED),
      )

      testAPIClient.runJob(JOB_EXPIRE_VISITS_FOR_REVIEW)

      val visitReview = visitReviewRepository.findByOfficialVisitId(visit.officialVisitId)
      visitReview[0].expiredTime isCloseTo LocalDateTime.now()
    }
  }
}
