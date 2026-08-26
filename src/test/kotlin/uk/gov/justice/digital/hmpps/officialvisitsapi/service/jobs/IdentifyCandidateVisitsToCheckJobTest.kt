package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class IdentifyCandidateVisitsToCheckJobTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val visitReviewQueueRepository: VisitReviewQueueRepository = mock()
  private val timeSource: TimeSource = TimeSource { LocalDateTime.now() }
  private val job: IdentifyCandidateVisitsToCheckJob = IdentifyCandidateVisitsToCheckJob(officialVisitRepository, visitReviewQueueRepository, timeSource)

  @Test
  fun `should call the decorated locations service when run`() {
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
    whenever { officialVisitRepository.findCandidateVisitsForReview(today, today.plusDays(7)) }
      .thenReturn(listOf(visit))

    job.runJob()

    verify(officialVisitRepository).findCandidateVisitsForReview(today, today.plusDays(7))
    verify(visitReviewQueueRepository).saveAndFlush(
      VisitReviewQueueEntity(
        officialVisitId = visit.officialVisitId,
        createdTime = timeSource.now(),
        triggeringEvent = "CRONJOB",
      ),
    )
  }
}
