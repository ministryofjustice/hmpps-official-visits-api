package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewDetailEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import java.time.LocalDateTime

class VisitReviewCheckerTest {
  private val visitReviewRepository: VisitReviewRepository = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val now = LocalDateTime.now()
  private val timeSource = TimeSource { now }
  private val checker = VisitReviewChecker(visitReviewRepository, prisonerSearchClient, timeSource)
  private val scheduledVisitAtMoorland = mock<OfficialVisitEntity>().stub {
    on { officialVisitId } doReturn 99
    on { prisonCode } doReturn MOORLAND
    on { prisonerNumber } doReturn MOORLAND_PRISONER.number
  }
  private val prisoner = mock<Prisoner>().stub {
    on { prisonId } doReturn MOORLAND
    on { status } doReturn "ACTIVE IN"
  }

  @Nested
  inner class VisitWithoutPreExistingIssues {
    private val visitReviewEntityCaptor = argumentCaptor<VisitReviewEntity>()

    @BeforeEach
    fun before() {
      visitReviewRepository.stub { on { findByOfficialVisitId(99) } doReturn emptyList() }
      prisonerSearchClient.stub { on { getPrisoner(MOORLAND_PRISONER.number) } doReturn prisoner }
    }

    @Test
    fun `should be no-op when no issues`() {
      checker.check(scheduledVisitAtMoorland)
      verify(visitReviewRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `should be release issue when prisoner released`() {
      prisoner.stub { on { status } doReturn "INACTIVE OUT" }
      checker.check(scheduledVisitAtMoorland)

      verify(visitReviewRepository).saveAndFlush(visitReviewEntityCaptor.capture())

      with(visitReviewEntityCaptor.firstValue) {
        officialVisitId isEqualTo 99
        raisedTime isEqualTo timeSource.now()
        visitReviewDetails().single().issueType isEqualTo IssueType.PRISONER_RELEASED
      }
    }

    @Test
    fun `should be transfer issue when prison is transferred`() {
      prisoner.stub { on { prisonId } doReturn PENTONVILLE }

      checker.check(scheduledVisitAtMoorland)

      verify(visitReviewRepository).saveAndFlush(visitReviewEntityCaptor.capture())

      with(visitReviewEntityCaptor.firstValue) {
        officialVisitId isEqualTo 99
        raisedTime isEqualTo timeSource.now()
        visitReviewDetails().single().issueType isEqualTo IssueType.PRISONER_TRANSFERRED
      }
    }
  }

  @Nested
  inner class VisitWithPreExistingIssues {
    private val visitReview: VisitReviewEntity = mock()
    private val visitReviewDetail: VisitReviewDetailEntity = mock()

    @BeforeEach
    fun before() {
      visitReviewRepository.stub { on { findByOfficialVisitId(99) } doReturn listOf(visitReview) }
      visitReview.stub { on { visitReviewDetails() } doReturn listOf(visitReviewDetail) }
      visitReviewDetail.stub { on { issueType } doReturn IssueType.VISITOR_NOT_APPROVED }
      prisonerSearchClient.stub { on { getPrisoner(MOORLAND_PRISONER.number) } doReturn prisoner }
    }

    @Test
    fun `should be no-op when no issues`() {
      checker.check(scheduledVisitAtMoorland)
      verify(visitReviewRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `should be release issue when prisoner is released`() {
      prisoner.stub { on { status } doReturn "INACTIVE OUT" }
      checker.check(scheduledVisitAtMoorland)

      verify(visitReviewRepository).saveAndFlush(visitReview)
      verify(visitReview).addVisitReviewDetails(timeSource.now(), IssueType.PRISONER_RELEASED, null)
    }

    @Test
    fun `should be no new release issue when prisoner is released and have existing unacknowledged release issue`() {
      prisoner.stub { on { status } doReturn "INACTIVE OUT" }
      visitReviewDetail.stub {
        on { issueType } doReturn IssueType.PRISONER_RELEASED
        on { acknowledgedBy } doReturn null
      }

      checker.check(scheduledVisitAtMoorland)

      verify(visitReviewRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `should be new release issue when prisoner is released and have existing acknowledged release issue`() {
      prisoner.stub { on { status } doReturn "INACTIVE OUT" }
      visitReviewDetail.stub {
        on { issueType } doReturn IssueType.PRISONER_RELEASED
        on { acknowledgedBy } doReturn "user"
      }

      checker.check(scheduledVisitAtMoorland)

      verify(visitReviewRepository).saveAndFlush(visitReview)
      verify(visitReview).addVisitReviewDetails(timeSource.now(), IssueType.PRISONER_RELEASED, null)
    }

    @Test
    fun `should be transfer issue when prisoner is transferred`() {
      prisoner.stub { on { prisonId } doReturn PENTONVILLE }

      checker.check(scheduledVisitAtMoorland)

      verify(visitReviewRepository).saveAndFlush(visitReview)
      verify(visitReview).addVisitReviewDetails(timeSource.now(), IssueType.PRISONER_TRANSFERRED, null)
    }

    @Test
    fun `should be no new transfer issue when prisoner is transferred and have existing unacknowledged release issue`() {
      prisoner.stub { on { prisonId } doReturn PENTONVILLE }
      visitReviewDetail.stub {
        on { issueType } doReturn IssueType.PRISONER_TRANSFERRED
        on { acknowledgedBy } doReturn null
      }

      checker.check(scheduledVisitAtMoorland)

      verify(visitReviewRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `should be new transfer issue when prisoner is transferred and have existing acknowledged release issue`() {
      prisoner.stub { on { prisonId } doReturn PENTONVILLE }
      visitReviewDetail.stub {
        on { issueType } doReturn IssueType.PRISONER_TRANSFERRED
        on { acknowledgedBy } doReturn "user"
      }

      checker.check(scheduledVisitAtMoorland)

      verify(visitReviewRepository).saveAndFlush(visitReview)
      verify(visitReview).addVisitReviewDetails(timeSource.now(), IssueType.PRISONER_TRANSFERRED, null)
    }
  }
}
