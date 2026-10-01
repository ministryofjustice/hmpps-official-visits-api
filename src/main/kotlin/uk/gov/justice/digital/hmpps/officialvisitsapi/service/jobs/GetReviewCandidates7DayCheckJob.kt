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
 * This job is responsible for identifying any visits occurring in 7 days time, as candidates to review.
 */
@Component
class GetReviewCandidates7DayCheckJob(
  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  private val featureSwitches: FeatureSwitches,
  private val transactionalPrisonJobProcessor: TransactionalPrisonJobProcessor,
) : VisitReviewJob<Long>(
  jobType = JobType.GET_REVIEW_CANDIDATES_7_DAY_CHECK,
  featureSwitches,
  transactionalPrisonJobProcessor,
  { prisonCode ->
    officialVisitRepository.findCandidateVisitsForReviewForPrison(LocalDate.now().plusDays(7), prisonCode)
  },
  { visitIds, _ ->
    visitIds.forEach { visitId ->
      visitReviewQueueRepository.saveAndFlush(
        VisitReviewQueueEntity(
          officialVisitId = visitId,
          createdTime = LocalDateTime.now(),
          triggeringEvent = VisitReviewCheckType.CHECK_7_DAYS,
        ),
      )
    }
  },

)
