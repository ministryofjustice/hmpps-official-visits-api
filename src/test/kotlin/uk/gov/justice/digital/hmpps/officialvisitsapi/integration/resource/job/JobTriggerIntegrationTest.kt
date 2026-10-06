package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.job

import org.awaitility.Awaitility.await
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.activeAlertForPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation2
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class JobTriggerIntegrationTest : IntegrationTestBase() {

  private companion object {
    private val createdTimeSlotIds = mutableListOf<Long>()
    private val createdVisitSlotIds = mutableListOf<Long>()
    private const val SEVEN_DAYS = 7L
    private const val TWO_DAYS = 2L
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
    if (createdTimeSlotIds.isNotEmpty()) {
      visitSlotRepository.deleteAllById(createdVisitSlotIds)
      timeSlotRepository.deleteAllById(createdTimeSlotIds)
      createdTimeSlotIds.clear()
      createdVisitSlotIds.clear()
    }
  }

  // ---------------------------------------------------------------------
  // Test data / fixture helpers
  // ---------------------------------------------------------------------

  /** Creates an official visit on a pre-built [VisitSlot] (e.g. one of the fixed [Moorland] slots). */
  private fun createVisitOnSlot(slot: VisitSlot, visitors: List<OfficialVisitor> = listOf(officialVisitor)) = testAPIClient.createOfficialVisit(createOfficialVisitRequest(slot, visitors), MOORLAND_PRISON_USER)

  /** Creates an official visit on [date], letting [testAPIClient] pick an available time slot. */
  private fun createVisitOnDate(date: LocalDate, visitors: List<OfficialVisitor> = listOf(officialVisitor)) = testAPIClient.generateVisitSlot(date).let { (timeSlot, visitSlot) ->
    createdTimeSlotIds.add(visitSlot.prisonTimeSlotId)
    createdVisitSlotIds.add(visitSlot.visitSlotId)
    createVisitOnSlot(VisitSlot(visitSlot.visitSlotId, date, timeSlot.startTime, timeSlot.endTime, moorlandLocation.id), visitors)
  }

  /** Creates an official visit on [date] at an explicit [startTime]/[endTime]. */
  private fun createVisitOnDateAndTimes(date: LocalDate, startTime: LocalTime, endTime: LocalTime, visitors: List<OfficialVisitor> = listOf(officialVisitor)) = testAPIClient.generateVisitSlot(date, startTime = startTime, endTime = endTime).let { (timeSlot, visitSlot) ->
    createdTimeSlotIds.add(visitSlot.prisonTimeSlotId)
    createdVisitSlotIds.add(visitSlot.visitSlotId)
    createVisitOnSlot(VisitSlot(visitSlot.visitSlotId, date, timeSlot.startTime, timeSlot.endTime, moorlandLocation.id), visitors)
  }

  /** A fixed slot that sits just outside the job's [SEVEN_DAYS] pickup window. */
  private fun slotBeyondCheckWindow() = VisitSlot(
    1,
    LocalDate.now().next(DayOfWeek.MONDAY).plusDays(SEVEN_DAYS),
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
    triggeringEvent: VisitReviewCheckType = VisitReviewCheckType.CHECK_7_DAYS,
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

  @Nested
  inner class GetReviewCandidates7DayCheckTest {

    @Test
    fun `should identify candidate visits to check in 7 days time`() {
      createVisitOnDate(LocalDate.now().plusDays(7))

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_7_DAY_CHECK.name)

      assertQueueSize(1)
    }

    @Test
    fun `should identify candidate visits to check in 7 days time multiple times`() {
      val officialVisit = createVisitOnDate(LocalDate.now().plusDays(7))
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(activeAlertForPrisoner(MOORLAND_PRISONER)))

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_7_DAY_CHECK.name)

      assertQueueSize(1)

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      assertQueueSize(0)

      val issues = firstReviewIssues()
      issues.size isEqualTo 2
      issues.map { it.issueType }.toList() containsExactlyInAnyOrder listOf(IssueType.PRISONER_NEW_ALERT, IssueType.PRISONER_RELEASED)

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_7_DAY_CHECK.name)

      assertQueueSize(0)

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      assertQueueSize(0)

      val issues2 = firstReviewIssues()
      issues2.size isEqualTo 2
      issues2.map { it.issueType }.toList() containsExactlyInAnyOrder listOf(IssueType.PRISONER_NEW_ALERT, IssueType.PRISONER_RELEASED)

      // Update the visit date to two days in the future
      val twoDaysInFuture = LocalDate.now().plusDays(TWO_DAYS)
      val updateVisitSlotRequest = OfficialVisitUpdateSlotRequest(
        prisonVisitSlotId = 1,
        visitDate = twoDaysInFuture,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        dpsLocationId = moorlandLocation.id,
        visitTypeCode = VisitType.VIDEO,
      )

      testAPIClient.updateSlot(
        MOORLAND_PRISONER.prison,
        officialVisitId = officialVisit.officialVisitId,
        request = updateVisitSlotRequest,
      )

      // since the visit was update after the alert was added, it does not raise it.
      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK.name)

      assertQueueSize(1)

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      assertQueueSize(0)

      val issues3 = firstReviewIssues()
      issues3.size isEqualTo 2
      issues3.map { it.issueType }.toList() containsExactlyInAnyOrder listOf(IssueType.PRISONER_NEW_ALERT, IssueType.PRISONER_RELEASED)
    }

    @Test
    fun `should not find candidates visits that are not scheduled for 7 days time`() {
      createVisitOnDate(LocalDate.now().plusDays(6))
      createVisitOnDate(LocalDate.now().plusDays(8))

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_7_DAY_CHECK.name)

      assertQueueSize(0)
    }

    @Test
    fun `should not find candidate visits if they are cancelled`() {
      val matchingVisit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      val cancelledVisit = createVisitOnSlot(Moorland.WEDNESDAY_9_TO_10_VISIT_SLOT)
      markVisitStatus(cancelledVisit.officialVisitId, VisitStatusType.CANCELLED)

      createVisitReview(
        officialVisitId = matchingVisit.officialVisitId,
        issueTypes = listOf(IssueType.VISITOR_NOT_APPROVED, IssueType.PRISONER_TRANSFERRED),
      )

      createVisitReview(cancelledVisit.officialVisitId)

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_7_DAY_CHECK.name)

      assertQueueSize(0)
    }
  }

  @Nested
  inner class GetReviewCandidates2DayCheckTest {

    @Test
    fun `should find candidates when they are scheduled for the day after tomorrow`() {
      createVisitOnDate(LocalDate.now().plusDays(TWO_DAYS))

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK.name)

      assertQueueSize(1)
    }

    @Test
    fun `should not find candidates when scheduled for less than 2 days, or or more than 2 days in the future`() {
      createVisitOnDate(LocalDate.now().plusDays(TWO_DAYS + 1))
      createVisitOnDate(LocalDate.now().plusDays(TWO_DAYS - 1))

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK.name)

      assertQueueSize(0)
    }

    @Test
    fun `should find candidates when they are cancelled or completed`() {
      val twoDaysInFuture = LocalDate.now().plusDays(TWO_DAYS)

      val cancelledVisit = createVisitOnDateAndTimes(
        twoDaysInFuture,
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
      )
      markVisitStatus(cancelledVisit.officialVisitId, VisitStatusType.CANCELLED)

      val completedVisit = createVisitOnDate(twoDaysInFuture)
      markVisitStatus(completedVisit.officialVisitId, VisitStatusType.COMPLETED)

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK.name)

      assertQueueSize(0)
    }
  }

  @Nested
  inner class ProcessReviewCandidatesTest {

    @Test
    fun `should process review candidates with no issues`() {
      val visit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      enqueueForReview(visit.officialVisitId)

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      assertQueueSize(0)
      reviewListContent() isEqualTo emptyList()
    }

    @Test
    fun `should process review candidates with new active prisoner alerts found`() {
      val visit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      enqueueForReview(visit.officialVisitId)
      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(activeAlertForPrisoner(MOORLAND_PRISONER)))

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      assertQueueSize(0)

      val issues = firstReviewIssues()
      issues.size isEqualTo 1
      issues[0].issueType isEqualTo IssueType.PRISONER_NEW_ALERT
    }

    @Test
    fun `should process review candidates with visitor-related issues`() {
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

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      assertQueueSize(0)

      val issues = firstReviewIssues()
      issues.size isEqualTo 1
      issues[0].issueType isEqualTo IssueType.VISITOR_NOT_OFFICIAL
    }

    @Test
    fun `should process review candidates with prisoner issues found`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val visit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      enqueueForReview(visit.officialVisitId)

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      assertQueueSize(0)

      val issues = firstReviewIssues()
      issues.size isEqualTo 1
      issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
    }

    @Test
    fun `should not process visits that are scheduled more than 7 days in the future`() {
      val visit = createVisitOnSlot(slotBeyondCheckWindow())
      enqueueForReview(visit.officialVisitId, createdTime = LocalDateTime.now().minusDays(SEVEN_DAYS))

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      assertQueueSize(0)
      reviewListContent() isEqualTo emptyList()
    }

    @Test
    fun `should process multiple review candidates with issues`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val twoDaysInFuture = LocalDate.now().plusDays(TWO_DAYS)

      // Create 2 visits
      createVisitOnDateAndTimes(
        twoDaysInFuture,
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
      )
      createVisitOnDate(twoDaysInFuture)

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK.name)
      assertQueueSize(2)

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)
      assertQueueSize(0)

      val reviews = reviewListContent()
      reviews[0].issues.size isEqualTo 1
      reviews[0].issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      reviews[1].issues.size isEqualTo 1
      reviews[1].issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
    }

    @Test
    fun `should process candidates with previously acknowledged issues`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)

      val twoDaysInFuture = LocalDate.now().plusDays(TWO_DAYS)
      val matchingVisit = createVisitOnDateAndTimes(
        twoDaysInFuture,
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
      )

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK.name)
      assertQueueSize(1)

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)
      assertQueueSize(0)

      firstReviewIssues().let { issues ->
        issues.size isEqualTo 1
        issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      }

      testAPIClient.acknowledgeVisitForReview(matchingVisit.officialVisitId, MOORLAND_PRISON_USER)
      reviewListContent() isEqualTo emptyList()

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK.name)
      assertQueueSize(1)

      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(activeAlertForPrisoner(MOORLAND_PRISONER)))
      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)
      assertQueueSize(0)

      await().untilCallTo {
        testAPIClient.getVisitsForReviewList().content.size isEqualTo 1
      }

      val issues = testAPIClient.getVisitsForReviewList().content[0].issues

      // The previous PRISONER_RELEASED issue has been acknowledged so only reports the new PRISONER_NEW_ALERT issue
      issues.size isEqualTo 1
      issues[0].issueType isEqualTo IssueType.PRISONER_NEW_ALERT
    }

    @Test
    fun `should process candidates to flag issues even if prior unacknowledged issues exist`() {
      prisonerSearchApi().stubGetPrisoner(MOORLAND_PRISONER_INACTIVE)
      val twoDaysInFuture = LocalDate.now().plusDays(TWO_DAYS)
      createVisitOnDateAndTimes(twoDaysInFuture, LocalTime.of(9, 0), LocalTime.of(10, 0))

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK.name)
      assertQueueSize(1)

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      assertQueueSize(0)
      firstReviewIssues().let { issues ->
        issues.size isEqualTo 1
        issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      }

      testAPIClient.runJob(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK.name)
      assertQueueSize(1)

      alertsApi().stubGetPrisonerAlerts(MOORLAND_PRISONER.number, listOf(activeAlertForPrisoner(MOORLAND_PRISONER)))

      testAPIClient.runJob(JobType.PROCESS_REVIEW_CANDIDATES.name)

      val issues = firstReviewIssues()
      issues.size isEqualTo 2
      issues[0].issueType isEqualTo IssueType.PRISONER_RELEASED
      issues[1].issueType isEqualTo IssueType.PRISONER_NEW_ALERT
    }
  }

  @Nested
  inner class ExpireReviewsJobTest {
    @Test
    fun `should expire reviews when the visit date is in the past`() {
      val visit = createVisitOnSlot(MONDAY_9_TO_10_VISIT_SLOT)
      setVisitDate(visit.officialVisitId, LocalDate.now().minusDays(20))

      createVisitReview(
        officialVisitId = visit.officialVisitId,
        issueTypes = listOf(IssueType.VISITOR_NOT_APPROVED, IssueType.PRISONER_TRANSFERRED),
      )

      testAPIClient.runJob(JobType.EXPIRE_VISIT_REVIEWS.name)

      val visitReview = visitReviewRepository.findByOfficialVisitId(visit.officialVisitId)
      visitReview[0].expiredTime isCloseTo LocalDateTime.now()
    }
  }
}
