package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class IdentifyCandidateVisitsToCheckJobTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val visitReviewQueueRepository: VisitReviewQueueRepository = mock()
  private val feature: FeatureSwitches = mock()
  private val timeSource: TimeSource = TimeSource { LocalDateTime.now() }
  private val job: IdentifyCandidateVisitsToCheckJob = IdentifyCandidateVisitsToCheckJob(officialVisitRepository, visitReviewQueueRepository, feature, timeSource)

  @Test
  fun `should call the find candidates visits service when run`() {
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
      visitDate = tomorrow(),
      startTime = LocalTime.of(11, 45),
      endTime = LocalTime.of(12, 45),
      dpsLocationId = UUID.randomUUID(),
      visitTypeCode = VisitType.IN_PERSON,
      createdBy = "unit test",
    )
    val today = timeSource.today()
    val prisonCodesList = setOf(PENTONVILLE)
    whenever { feature.getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null) }
      .thenReturn(PENTONVILLE)
    whenever { officialVisitRepository.findCandidateVisitsForReview(today.plusDays(7), prisonCodesList) }
      .thenReturn(listOf(visit.officialVisitId))

    job.runJob()

    verify(officialVisitRepository).findCandidateVisitsForReview(today.plusDays(7), prisonCodesList)
    verify(visitReviewQueueRepository).saveAndFlush(
      VisitReviewQueueEntity(
        officialVisitId = visit.officialVisitId,
        createdTime = timeSource.now(),
        triggeringEvent = VisitReviewCheckType.CHECK,
      ),
    )
  }
}
