package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService
import java.time.LocalTime
import java.util.UUID

class ProcessCandidateVisitsToCheckJobTest {
  private val visitReviewQueueRepository: VisitReviewQueueRepository = mock()
  private val visitReviewService: VisitReviewService = mock()
  private val features: FeatureSwitches = mock()
  private val transactionalPrisonJobProcessor = TransactionalPrisonJobProcessor()
  private val job: ProcessCandidateVisitsToCheckJob = ProcessCandidateVisitsToCheckJob(visitReviewQueueRepository, visitReviewService, features, transactionalPrisonJobProcessor)

  @Test
  fun `should call the find candidates visits service when run for each prison`() {
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
    val queueEntry = VisitReviewQueueEntity(
      visitReviewQueueId = 1,
      officialVisitId = visit.officialVisitId,
      createdTime = now(),
      triggeringEvent = VisitReviewCheckType.UPDATE,
    )
    whenever { features.getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null) }
      .thenReturn(PENTONVILLE)
    whenever { visitReviewQueueRepository.findCandidatesOrderedByQueueTimeForPrison(PENTONVILLE) }
      .thenReturn(listOf(queueEntry))

    job.runJob()

    verify(visitReviewQueueRepository).findCandidatesOrderedByQueueTimeForPrison(PENTONVILLE)
    verify(visitReviewService, times(1)).visitCheckInNewTransaction(visit.officialVisitId, VisitReviewCheckType.UPDATE)
  }

  @Test
  fun `should process multiple prisons with separate transactions`() {
    val prisonCode1 = "MDI"
    val prisonCode2 = "LEI"
    val queueEntry1 = VisitReviewQueueEntity(
      visitReviewQueueId = 1,
      officialVisitId = 1L,
      createdTime = now(),
      triggeringEvent = VisitReviewCheckType.CHECK,
    )
    val queueEntry2 = VisitReviewQueueEntity(
      visitReviewQueueId = 2,
      officialVisitId = 2L,
      createdTime = now(),
      triggeringEvent = VisitReviewCheckType.RECHECK,
    )

    whenever { features.getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null) }
      .thenReturn("$prisonCode1,$prisonCode2")
    whenever { visitReviewQueueRepository.findCandidatesOrderedByQueueTimeForPrison(prisonCode1) }
      .thenReturn(listOf(queueEntry1))
    whenever { visitReviewQueueRepository.findCandidatesOrderedByQueueTimeForPrison(prisonCode2) }
      .thenReturn(listOf(queueEntry2))

    job.runJob()

    verify(visitReviewQueueRepository).findCandidatesOrderedByQueueTimeForPrison(prisonCode1)
    verify(visitReviewQueueRepository).findCandidatesOrderedByQueueTimeForPrison(prisonCode2)
    verify(visitReviewService).visitCheckInNewTransaction(1L, VisitReviewCheckType.CHECK)
    verify(visitReviewService).visitCheckInNewTransaction(2L, VisitReviewCheckType.RECHECK)
  }
}
