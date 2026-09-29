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
 * This job is responsible for identifying the visits that need to be reviewed.
 *
 * Visits will be checked and flagged for review.
 * Processing is done per-prison, with each prison's visits handled in a separate transaction.
 */
@Component
class IdentifyCandidateVisitsToCheckJob(

  private val officialVisitRepository: OfficialVisitRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  features: FeatureSwitches,
  transactionalPrisonJobProcessor: TransactionalPrisonJobProcessor,
) : PrisonAwareDailyJob<Long>(
  jobType = JobType.IDENTIFY_CANDIDATE_VISITS_TO_CHECK,
  features,
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
          triggeringEvent = VisitReviewCheckType.CHECK,
        ),
      )
    }
  },

)
