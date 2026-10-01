package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import java.time.LocalTime
import java.util.UUID

class GetReviewCandidates2DayCheckJobTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val visitReviewQueueRepository: VisitReviewQueueRepository = mock()
  private val feature: FeatureSwitches = mock()
  private val transactionalPrisonJobProcessor = TransactionalPrisonJobProcessor()
  private val job: GetReviewCandidates2DayCheckJob = GetReviewCandidates2DayCheckJob(officialVisitRepository, visitReviewQueueRepository, feature, transactionalPrisonJobProcessor)

  @Test
  fun `should add day after tomorrow candidate visits to the queue as rechecks for each prison`() {
    val prisonVisitSlot = PrisonVisitSlotEntity(
      prisonVisitSlotId = 1,
      prisonTimeSlotId = 1,
      dpsLocationId = UUID.randomUUID(),
      maxAdults = 1,
      maxGroups = 1,
      maxVideoSessions = 1,
      createdBy = "unit test",
      createdTime = now(),
    )
    val visit = OfficialVisitEntity(
      prisonVisitSlot = prisonVisitSlot,
      prisonCode = PENTONVILLE,
      prisonerNumber = PENTONVILLE_PRISONER.number,
      visitDate = tomorrow().plusDays(1),
      startTime = LocalTime.of(11, 45),
      endTime = LocalTime.of(12, 45),
      dpsLocationId = UUID.randomUUID(),
      visitTypeCode = VisitType.IN_PERSON,
      createdBy = "unit test",
    )
    val today = today()
    whenever { feature.getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null) }
      .thenReturn(PENTONVILLE)
    whenever { officialVisitRepository.findCandidateVisitsForReReviewForPrison(today.plusDays(2), PENTONVILLE) }
      .thenReturn(listOf(visit.officialVisitId))

    job.runJob()

    verify(officialVisitRepository).findCandidateVisitsForReReviewForPrison(today.plusDays(2), PENTONVILLE)
    verify(visitReviewQueueRepository).saveAndFlush(
      org.mockito.kotlin.check {
        it.officialVisitId == visit.officialVisitId && it.triggeringEvent == VisitReviewCheckType.CHECK_2_DAYS
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
    whenever { officialVisitRepository.findCandidateVisitsForReReviewForPrison(today.plusDays(2), prisonCode1) }
      .thenReturn(listOf(visitId1))
    whenever { officialVisitRepository.findCandidateVisitsForReReviewForPrison(today.plusDays(2), prisonCode2) }
      .thenReturn(listOf(visitId2))

    job.runJob()

    verify(officialVisitRepository).findCandidateVisitsForReReviewForPrison(today.plusDays(2), prisonCode1)
    verify(officialVisitRepository).findCandidateVisitsForReReviewForPrison(today.plusDays(2), prisonCode2)
    val captor = org.mockito.kotlin.argumentCaptor<VisitReviewQueueEntity>()
    org.mockito.kotlin.verify(visitReviewQueueRepository, org.mockito.kotlin.times(2)).saveAndFlush(captor.capture())
    val savedIds = captor.allValues.map { it.officialVisitId }
    assertTrue(savedIds.containsAll(listOf(visitId1, visitId2)))
  }
}
