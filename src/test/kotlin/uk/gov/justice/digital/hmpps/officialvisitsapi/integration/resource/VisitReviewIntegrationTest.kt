package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation2
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import java.time.LocalDateTime

class VisitReviewIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var visitReviewRepository: VisitReviewRepository

  // todo move to common place
  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 123,
    prisonerContactId = 456,
    leadVisitor = true,
  )

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearReviewAndVisitData()
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
  @Transactional
  fun tearDown() {
    clearReviewAndVisitData()
  }

  @Test
  fun `should get count of scheduled future unacknowledged and unexpired visits for review`() {
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

    createVisitReview(matchingVisit.officialVisitId)
    createVisitReview(cancelledVisit.officialVisitId)

    val response = testAPIClient.getVisitsForReviewCount(MOORLAND_PRISON_USER)

    response.prisonCode isEqualTo MOORLAND
    response.visitsForReviewCount isEqualTo 1L
  }

  @Test
  fun `should count a visit raising several issues only once`() {
    val visit = testAPIClient.createOfficialVisit(
      createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
      MOORLAND_PRISON_USER,
    )

    createVisitReview(
      visit.officialVisitId,
      issueTypes = listOf(IssueType.PRISONER_RELEASED, IssueType.VISITOR_NOT_APPROVED),
    )

    val response = testAPIClient.getVisitsForReviewCount(MOORLAND_PRISON_USER)

    response.visitsForReviewCount isEqualTo 1L
  }

  @Test
  fun `should get current visits for review with grouped issues`() {
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

    val pageOne = testAPIClient.getVisitsForReviewList()

    with(pageOne) {
      val response = content.get(0)
      response.visit.officialVisitId isEqualTo matchingVisit.officialVisitId
      response.visit.prisonerVisited?.prisonerNumber isEqualTo MOORLAND_PRISONER.number
      response.visit.locationDescription isEqualTo moorlandLocation.localName
      response.issues.size isEqualTo 2
      response.issues[0].issueType isEqualTo IssueType.VISITOR_NOT_APPROVED
      response.issues[1].issueType isEqualTo IssueType.PRISONER_TRANSFERRED
      page.size isEqualTo 10
      page.number isEqualTo 0
      page.totalElements isEqualTo 1
      page.totalPages isEqualTo 1
    }
  }

  @Test
  fun `should get empty response with headers for visit review list`() {
    val pageOne = testAPIClient.getVisitsForReviewList()

    with(pageOne) {
      content isEqualTo emptyList()
      page.size isEqualTo 10
      page.number isEqualTo 0
      page.totalElements isEqualTo 0
      page.totalPages isEqualTo 0
    }
  }

  @Test
  fun `should acknowledge visit for review`() {
    val matchingVisit = testAPIClient.createOfficialVisit(
      createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor)),
      MOORLAND_PRISON_USER,
    )

    createVisitReview(matchingVisit.officialVisitId)
    val before = testAPIClient.getVisitsForReviewList()
    with(before) {
      content.size isEqualTo 1
      page.totalElements isEqualTo 1
    }

    testAPIClient.acknowledgeVisitForReview(matchingVisit.officialVisitId, MOORLAND_PRISON_USER)

    val after = testAPIClient.getVisitsForReviewList()
    with(after) {
      content isEqualTo emptyList()
    }
  }

  // todo move to common place
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

  private fun clearReviewAndVisitData() {
    visitReviewRepository.deleteAll()
    clearAllVisitData()
  }
}
