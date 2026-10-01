package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService

class ExpireVisitReviewsJobTest {

  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val visitReviewService: VisitReviewService = mock()
  private val features: FeatureSwitches = mock()
  private val prisonProcessor = PrisonProcessor()
  private val job = ExpireVisitReviewsJob(officialVisitRepository, visitReviewService, features, prisonProcessor)

  @Test
  fun `should expire visits for review for each prison`() {
    val visitId1 = 1L
    val today = today()
    whenever(features.getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null))
      .thenReturn(PENTONVILLE)
    whenever(officialVisitRepository.findOverdueVisitsWithUnacknowledgedReviewDetailsBeforeForPrison(today, PENTONVILLE))
      .thenReturn(listOf(visitId1))

    job.runJob()

    verify(officialVisitRepository).findOverdueVisitsWithUnacknowledgedReviewDetailsBeforeForPrison(today, PENTONVILLE)
    verify(visitReviewService).expire(visitId1)
  }

  @Test
  fun `should process multiple prisons with separate transactions`() {
    val prisonCode1 = "MDI"
    val prisonCode2 = "LEI"
    val today = today()
    val visitId1 = 1L
    val visitId2 = 2L

    whenever(features.getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null))
      .thenReturn("$prisonCode1,$prisonCode2")
    whenever(officialVisitRepository.findOverdueVisitsWithUnacknowledgedReviewDetailsBeforeForPrison(today, prisonCode1))
      .thenReturn(listOf(visitId1))
    whenever(officialVisitRepository.findOverdueVisitsWithUnacknowledgedReviewDetailsBeforeForPrison(today, prisonCode2))
      .thenReturn(listOf(visitId2))

    job.runJob()

    verify(officialVisitRepository).findOverdueVisitsWithUnacknowledgedReviewDetailsBeforeForPrison(today, prisonCode1)
    verify(officialVisitRepository).findOverdueVisitsWithUnacknowledgedReviewDetailsBeforeForPrison(today, prisonCode2)
    verify(visitReviewService).expire(visitId1)
    verify(visitReviewService).expire(visitId2)
  }
}
