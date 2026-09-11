package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType

@Component
class IdentifyCandidateVisitsToReCheckJob(

  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  timeSource: TimeSource,
) : DailyJob<OfficialVisitEntity>(
  jobType = JobType.IDENTIFY_CANDIDATE_VISITS_TO_RECHECK,
  timeSource,
  { date ->
    officialVisitRepository.findCandidateVisitsForReReview(date.plusDays(2))
  },
  { visits ->
    visits.forEach {
      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = it.officialVisitId,
          createdTime = timeSource.now(),
          triggeringEvent = VisitReviewCheckType.RECHECK,
        ),
      )
    }
  },
)
