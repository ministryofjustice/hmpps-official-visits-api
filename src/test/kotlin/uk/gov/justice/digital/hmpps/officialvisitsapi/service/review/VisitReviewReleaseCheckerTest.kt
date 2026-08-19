package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewDetailEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import java.time.LocalDateTime

class VisitReviewReleaseCheckerTest {
  private val visitReviewRepository: VisitReviewRepository = mock()
  private val now = LocalDateTime.now()
  private val timeSource = TimeSource { now }
  private val releaseChecker = VisitReviewReleaseChecker(visitReviewRepository, timeSource)

  private val scheduledVisit = mock<OfficialVisitEntity>().stub { on { officialVisitId } doReturn 99 }
  private val visitReview: VisitReviewEntity = mock()
  private val visitReviewDetail: VisitReviewDetailEntity = mock()

  @Test
  fun `should be no new release details when pre-existing release visit review details`() {
    visitReviewDetail.stub { on { issueType } doReturn IssueType.PRISONER_RELEASED }
    visitReview.stub { on { visitReviewDetails() } doReturn listOf(visitReviewDetail) }
    visitReviewRepository.stub { on { findByOfficialVisitId(99) } doReturn listOf(visitReview) }

    releaseChecker.check(scheduledVisit)

    verify(visitReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should be new release details when no pre-existing visit review details`() {
    visitReviewDetail.stub { on { issueType } doReturn IssueType.VISITOR_NOT_APPROVED }
    visitReview.stub { on { visitReviewDetails() } doReturn listOf(visitReviewDetail) }
    visitReviewRepository.stub { on { findByOfficialVisitId(99) } doReturn listOf(visitReview) }

    releaseChecker.check(scheduledVisit)

    verify(visitReviewRepository).saveAndFlush(visitReview)
    verify(visitReview).addVisitReviewDetails(timeSource.now(), IssueType.PRISONER_RELEASED, null)
  }

  @Test
  fun `should be new release details when no pre-existing visit review`() {
    val visitReviewEntityCaptor = argumentCaptor<VisitReviewEntity>()
    visitReviewRepository.stub { on { findByOfficialVisitId(99) } doReturn emptyList() }

    releaseChecker.check(scheduledVisit)

    verify(visitReviewRepository).saveAndFlush(visitReviewEntityCaptor.capture())

    with(visitReviewEntityCaptor.firstValue) {
      officialVisitId isEqualTo 99
      raisedTime isEqualTo timeSource.now()
      visitReviewDetails().single().issueType isEqualTo IssueType.PRISONER_RELEASED
    }
  }
}
