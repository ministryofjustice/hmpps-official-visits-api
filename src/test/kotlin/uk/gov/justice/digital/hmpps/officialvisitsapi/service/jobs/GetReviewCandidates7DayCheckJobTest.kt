package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType

class GetReviewCandidates7DayCheckJobTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val visitReviewQueueRepository: VisitReviewQueueRepository = mock()
  private val feature: FeatureSwitches = mock()
  private val prisonProcessor = PrisonProcessor()
  private val job: GetReviewCandidates7DayCheckJob = GetReviewCandidates7DayCheckJob(officialVisitRepository, visitReviewQueueRepository, feature, prisonProcessor)

  @Test
  fun `should call the find candidates visits service when run for each prison`() {
    val visitId1 = 1L
    val today = today()
    whenever { feature.getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null) }
      .thenReturn(PENTONVILLE)
    whenever { officialVisitRepository.findScheduledUnreviewedVisitsForPrisonOnDate(today.plusDays(7), PENTONVILLE) }
      .thenReturn(listOf(visitId1))

    job.runJob()

    verify(officialVisitRepository).findScheduledUnreviewedVisitsForPrisonOnDate(today.plusDays(7), PENTONVILLE)
    verify(visitReviewQueueRepository).saveAndFlush(
      check {
        it.officialVisitId == visitId1 && it.triggeringEvent == VisitReviewCheckType.CHECK_7_DAYS
      },
    )
  }

  @Test
  fun `should process multiple prisons with separate transactions`() {
    val prisonCode1 = "MDI"
    val prisonCode2 = "LEI"
    val today = today()
    val visitId1 = 1L
    val visitId2 = 2L

    whenever { feature.getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null) }
      .thenReturn("$prisonCode1,$prisonCode2")
    whenever { officialVisitRepository.findScheduledUnreviewedVisitsForPrisonOnDate(today.plusDays(7), prisonCode1) }
      .thenReturn(listOf(visitId1))
    whenever { officialVisitRepository.findScheduledUnreviewedVisitsForPrisonOnDate(today.plusDays(7), prisonCode2) }
      .thenReturn(listOf(visitId2))

    job.runJob()

    verify(officialVisitRepository).findScheduledUnreviewedVisitsForPrisonOnDate(today.plusDays(7), prisonCode1)
    verify(officialVisitRepository).findScheduledUnreviewedVisitsForPrisonOnDate(today.plusDays(7), prisonCode2)

    val captor = argumentCaptor<VisitReviewQueueEntity>()
    verify(visitReviewQueueRepository, org.mockito.kotlin.times(2)).saveAndFlush(captor.capture())
    val savedIds = captor.allValues.map { it.officialVisitId }
    assertTrue(savedIds.containsAll(listOf(visitId1, visitId2)))
  }
}
