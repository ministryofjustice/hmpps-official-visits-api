package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository

/**
 * This job is responsible for identifying the visits that need reviewed.
 *
 * Visits  will be checked and flagged for review.
 */
@Component
class IdentifyCandidateVisitsToCheckJob(

  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  timeSource: TimeSource,
) : DailyJob(
  jobType = JobType.IDENTIFY_CANDIDATE_VISITS_TO_CHECK,
  timeSource,
  { date ->
    officialVisitRepository.findCandidateVisitsForReview(date, date.plusDays(7))
  },
  { visits ->
    visits.forEach {
      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = it.officialVisitId,
          createdTime = timeSource.now(),
          triggeringEvent = "CRONJOB",
        ),
      )
    }
  },

)
