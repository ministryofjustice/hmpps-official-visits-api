package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PrisonerContactDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactRelationship
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.SummaryRelationship
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository

class VisitsWithApprovalIssuesTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val contactsService: ContactsService = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val visitsWithApprovalIssues = VisitsWithApprovalIssues(officialVisitRepository, contactsService, featureSwitches)
  private val visit: OfficialVisitEntity = createAVisitEntity(1)
  private val visitId = visit.officialVisitId
  private val prisonerNumber = visit.prisonerNumber
  private val firstContactId = visit.officialVisitors().first().contactId!!
  private val secondContactId = visit.officialVisitors().last().contactId!!

  @BeforeEach
  fun beforeEach() {
    whenever { featureSwitches.getValue(StringFeature.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS) } doReturn MOORLAND
  }

  @Test
  fun `should be no issues for candidate visit`() {
    whenever { officialVisitRepository.findAllById(listOf(1)) } doReturn listOf(visit)

    contactsService.stub {
      on { getPrisonerContactRelationships(setOf(PrisonerContactDto(prisonerNumber, firstContactId), PrisonerContactDto(prisonerNumber, secondContactId))) } doReturn mapOf(prisonerNumber to listOf(relationship(prisonerNumber, firstContactId), relationship(prisonerNumber, secondContactId)))
    }

    visitsWithApprovalIssues.identify(MOORLAND, listOf(visitId)) hasSize 0

    verify(officialVisitRepository).findAllById(listOf(visitId))
    verify(contactsService).getPrisonerContactRelationships(setOf(PrisonerContactDto(prisonerNumber, firstContactId), PrisonerContactDto(prisonerNumber, secondContactId)))
  }

  @Test
  fun `should be issues for candidate visit when no contact relationships found`() {
    whenever { officialVisitRepository.findAllById(listOf(1)) } doReturn listOf(visit)

    contactsService.stub {
      on { getPrisonerContactRelationships(setOf(PrisonerContactDto(prisonerNumber, firstContactId), PrisonerContactDto(prisonerNumber, secondContactId))) } doReturn emptyMap()
    }

    visitsWithApprovalIssues.identify(MOORLAND, listOf(visitId)) hasSize 1

    verify(officialVisitRepository).findAllById(listOf(visitId))
    verify(contactsService).getPrisonerContactRelationships(setOf(PrisonerContactDto(prisonerNumber, firstContactId), PrisonerContactDto(prisonerNumber, secondContactId)))
  }

  @Test
  fun `should be issues for candidate visit when contact not approved`() {
    whenever { officialVisitRepository.findAllById(listOf(1)) } doReturn listOf(visit)

    contactsService.stub {
      on { getPrisonerContactRelationships(setOf(PrisonerContactDto(prisonerNumber, firstContactId), PrisonerContactDto(prisonerNumber, secondContactId))) } doReturn mapOf(prisonerNumber to listOf(relationship(prisonerNumber, firstContactId, isApproved = false), relationship(prisonerNumber, secondContactId)))
    }

    visitsWithApprovalIssues.identify(MOORLAND, listOf(visitId)) hasSize 1

    verify(officialVisitRepository).findAllById(listOf(visitId))
    verify(contactsService).getPrisonerContactRelationships(setOf(PrisonerContactDto(prisonerNumber, firstContactId), PrisonerContactDto(prisonerNumber, secondContactId)))
  }

  @Test
  fun `should be issues for candidate visit when social visits are not allowed`() {
    whenever { featureSwitches.getValue(StringFeature.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS) } doReturn null
    whenever { officialVisitRepository.findAllById(listOf(1)) } doReturn listOf(visit)

    contactsService.stub {
      on { getPrisonerContactRelationships(setOf(PrisonerContactDto(prisonerNumber, firstContactId), PrisonerContactDto(prisonerNumber, secondContactId))) } doReturn mapOf(prisonerNumber to listOf(relationship(prisonerNumber, firstContactId, relationshipType = "S"), relationship(prisonerNumber, secondContactId)))
    }

    visitsWithApprovalIssues.identify(MOORLAND, listOf(visitId)) hasSize 1

    verify(officialVisitRepository).findAllById(listOf(visitId))
    verify(contactsService).getPrisonerContactRelationships(setOf(PrisonerContactDto(prisonerNumber, firstContactId), PrisonerContactDto(prisonerNumber, secondContactId)))
  }

  private fun relationship(prisonerNumber: String, contactId: Long, relationshipType: String = "S", isApproved: Boolean = true) = PrisonerContactRelationship(
    prisonerNumber = prisonerNumber,
    contactId = contactId,
    relationships = listOf(
      SummaryRelationship(
        prisonerContactId = contactId,
        relationshipTypeCode = relationshipType,
        relationshipToPrisonerCode = "FRI",
        isApprovedVisitor = isApproved,
        isRelationshipActive = true,
        currentTerm = true,
      ),
    ),
  )
}
