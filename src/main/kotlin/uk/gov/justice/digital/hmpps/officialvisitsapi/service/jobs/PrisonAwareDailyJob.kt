package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import java.time.LocalDate

/**
 * Base class for daily jobs that process visits on a per-prison basis.
 * Each prison's processing runs in its own transaction, providing better isolation
 * and reducing the chance of long-running transactions.
 *
 * @param T The type of data supplied for processing (e.g., Long for visit IDs, VisitReviewQueueEntity)
 */
abstract class PrisonAwareDailyJob<T>(
  jobType: JobType,
  private val timeSource: TimeSource,
  private val featureSwitches: FeatureSwitches,
  private val prisonJobProcessor: PrisonJobProcessor,
  private val supplier: (LocalDate, String) -> Collection<T>,
  private val consumer: (Collection<T>, String) -> Unit,
) : JobDefinition(
  jobType,
  {
    val featureEnabledPrisonCodesList = featureSwitches
      .getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null)
      ?.split(',')
      ?.map { it.trim() }
      ?.toSet()
      ?: emptySet()

    // Process each prison in its own transaction
    // Transactions are performed by the PrisonJobProcessor bean to ensure Spring proxies apply @Transactional
    featureEnabledPrisonCodesList.forEach { prisonCode ->
      prisonJobProcessor.processForPrison(prisonCode, timeSource, supplier, consumer)
    }
  },
)
