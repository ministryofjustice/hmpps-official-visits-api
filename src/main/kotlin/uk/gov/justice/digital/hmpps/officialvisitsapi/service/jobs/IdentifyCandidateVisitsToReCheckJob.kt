package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType

@Component
class IdentifyCandidateVisitsToReCheckJob(

  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  timeSource: TimeSource,
) : DailyJob<Long>(
  jobType = JobType.IDENTIFY_CANDIDATE_VISITS_TO_RECHECK,
  timeSource,
  { date ->
    officialVisitRepository.findCandidateVisitsForReview(date.plusDays(2), VisitReviewCheckType.RECHECK)
  },
  { officialVisitId ->
    officialVisitId.forEach {
      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = it,
          createdTime = timeSource.now(),
          triggeringEvent = VisitReviewCheckType.RECHECK,
        ),
      )
    }
  },
)
