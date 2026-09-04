package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toPrisonerContactModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService

class VisitorIssueCheckerTest {

  private lateinit var contactsService: ContactsService
  private lateinit var featureSwitches: FeatureSwitches
  private lateinit var checker: VisitorIssueChecker

  private val prisonerNumber = "A1234BC"

  @BeforeEach
  fun setUp() {
    contactsService = mock(ContactsService::class.java)
    featureSwitches = mock(FeatureSwitches::class.java)
    checker = VisitorIssueChecker(contactsService, featureSwitches)
  }

  private fun visitor(contactId: Long, firstName: String = "Jane", lastName: String = "Doe"): OfficialVisitorEntity {
    val v = mock(OfficialVisitorEntity::class.java)
    whenever(v.contactId).thenReturn(contactId)
    whenever(v.firstName).thenReturn(firstName)
    whenever(v.lastName).thenReturn(lastName)
    return v
  }

  private fun officialVisit(vararg visitors: OfficialVisitorEntity): OfficialVisitEntity {
    val visit = mock(OfficialVisitEntity::class.java)
    whenever(visit.prisonerNumber).thenReturn(prisonerNumber)
    whenever(visit.officialVisitors()).thenReturn(visitors.toList())
    return visit
  }

  @Test
  fun `returns no issues when visitor has official approved relationship`() {
    val visit = officialVisit(visitor(contactId = 1))
    whenever(contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true))
      .thenReturn(listOf(getPrisonerContacts(contactId = 1)))

    val issues = checker.checkVisitorIssues(visit)

    assertTrue(issues.isEmpty())
  }

  @Test
  fun `adds a single issue when one visitor has no known relationship`() {
    val visit = officialVisit(visitor(contactId = 1))
    whenever(contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true))
      .thenReturn(emptyList())

    val issues = checker.checkVisitorIssues(visit)

    assertEquals(1, issues.size)
    assertEquals(IssueType.VISITOR_NO_RELATIONSHIP, issues.single().issueType)
  }

  @Test
  fun `collapses multiple visitors with no relationship into a single issue row`() {
    val visit = officialVisit(
      visitor(contactId = 1, firstName = "Alice"),
      visitor(contactId = 2, firstName = "Bob"),
    )
    whenever(contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true))
      .thenReturn(emptyList())

    val issues = checker.checkVisitorIssues(visit)

    assertEquals(1, issues.size)
    assertEquals(IssueType.VISITOR_NO_RELATIONSHIP, issues.single().issueType)
  }

  @Test
  fun `adds social relationship issue when visitor's relationship is social and feature flag is disabled`() {
    val visit = officialVisit(visitor(contactId = 1))
    whenever(contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true))
      .thenReturn(listOf(getPrisonerContacts(contactId = 1, relationshipTypeCode = "S")))
    whenever { featureSwitches.getValue(StringFeature.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS, null) } doReturn null

    val issues = checker.checkVisitorIssues(visit)

    assertEquals(1, issues.size)
    assertEquals(IssueType.VISITOR_NOT_OFFICIAL, issues.single().issueType)
  }

  @Test
  fun `adds not-approved issue when visitor is not an approved visitor`() {
    val visit = officialVisit(visitor(contactId = 1))
    whenever(contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true))
      .thenReturn(listOf(getPrisonerContacts(contactId = 1, isApprovedVisitor = false)))

    val issues = checker.checkVisitorIssues(visit)

    assertEquals(1, issues.size)
    assertEquals(IssueType.VISITOR_NOT_APPROVED, issues.single().issueType)
  }

  @Test
  fun `adds both not-official and not-approved issues for the same visitor when both apply`() {
    val visit = officialVisit(visitor(contactId = 1))

    whenever(contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true))
      .thenReturn(
        listOf(
          getPrisonerContacts(contactId = 1, relationshipTypeCode = "S", isApprovedVisitor = false),
        ),
      )

    val issues = checker.checkVisitorIssues(visit)

    assertEquals(2, issues.size)
    val issueTypes = issues.map { it.issueType }
    assertTrue(issueTypes.contains(IssueType.VISITOR_NOT_OFFICIAL))
    assertTrue(issueTypes.contains(IssueType.VISITOR_NOT_APPROVED))
  }

  @Test
  fun `combines a no-relationship visitor with issues from a different, known visitor`() {
    val visit = officialVisit(
      visitor(contactId = 1), // unknown to personal-relationships-api
      visitor(contactId = 2), // known, but not approved
    )
    whenever(contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true))
      .thenReturn(listOf(getPrisonerContacts(contactId = 2, isApprovedVisitor = false)))

    val issues = checker.checkVisitorIssues(visit)

    assertEquals(2, issues.size)
    val issueTypes = issues.map { it.issueType }
    assertTrue(issueTypes.contains(IssueType.VISITOR_NO_RELATIONSHIP))
    assertTrue(issueTypes.contains(IssueType.VISITOR_NOT_APPROVED))
  }

  @Test
  fun `combines multiple issues from multiple visitors`() {
    val visit = officialVisit(
      visitor(contactId = 1), // unknown to personal-relationships-api
      visitor(contactId = 2), // known, but not approved
      visitor(contactId = 3), // known, but not official
    )
    whenever(contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true))
      .thenReturn(
        listOf(
          getPrisonerContacts(contactId = 2, isApprovedVisitor = false),
          getPrisonerContacts(contactId = 3, relationshipTypeCode = "S"),
        ),
      )
    val issues = checker.checkVisitorIssues(visit)
    assertEquals(3, issues.size)
  }

  @Test
  fun `all issues are tagged with the correct visitId`() {
    val visit = officialVisit(visitor(contactId = 1))
    whenever(contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true))
      .thenReturn(listOf(getPrisonerContacts(contactId = 1, isApprovedVisitor = false)))

    val issues = checker.checkVisitorIssues(visit)

    assertTrue(issues.all { it.visitId == visit.officialVisitId })
  }

  fun getPrisonerContacts(
    contactId: Long = 7L,
    relationshipTypeCode: String = "O",
    isApprovedVisitor: Boolean = true,
  ): PrisonerContact {
    val returnedContact1 = prisonerContact(
      prisonerNumber = MOORLAND_PRISONER.number,
      type = relationshipTypeCode,
      contactId = contactId,
      prisonerContactId = 700L,
      firstName = "Jane",
      lastName = "Doe",
      isApprovedVisitor = isApprovedVisitor,
    ).toPrisonerContactModel()

    return returnedContact1
  }
}
