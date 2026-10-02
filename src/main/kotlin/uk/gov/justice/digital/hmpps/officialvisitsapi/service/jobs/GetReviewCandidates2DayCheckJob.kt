package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * This job is responsible for finding visits due to take place in 2 days time.
 * Visits found are added to a table of candidates for review.
 */
@Component
class GetReviewCandidates2DayCheckJob(
  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  private val featureSwitches: FeatureSwitches,
  private val prisonProcessor: PrisonProcessor,
) : VisitReviewJob<Long>(
  jobType = JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK,
  featureSwitches,
  prisonProcessor,
  { prisonCode ->
    officialVisitRepository.findScheduledVisitsForPrisonOnDate(LocalDate.now().plusDays(2), prisonCode)
  },
  { visitIds, _ ->
    visitIds.forEach { visitId ->
      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visitId,
          createdTime = LocalDateTime.now(),
          triggeringEvent = VisitReviewCheckType.CHECK_2_DAYS,
        ),
      )
    }
  },
)
