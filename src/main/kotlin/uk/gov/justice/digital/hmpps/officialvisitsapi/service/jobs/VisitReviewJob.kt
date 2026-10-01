package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature

/**
 * Base class for jobs which are related to the visits for review feature.
 * The visit review feature switches determine which prisons, if any, the job will run for.
 * @param T The type of data supplied for processing (e.g., Long for visit IDs, VisitReviewQueueEntity)
 */
abstract class VisitReviewJob<T>(
  jobType: JobType,
  private val featureSwitches: FeatureSwitches,
  private val transactionalPrisonJobProcessor: TransactionalPrisonJobProcessor,
  private val supplier: (String) -> Collection<T>,
  private val consumer: (Collection<T>, String) -> Unit,
) : JobDefinition(
  jobType,
  {
    val featureEnabledPrisonCodes = featureSwitches
      .getValue(StringFeature.FEATURE_VISITS_NEED_REVIEW_PRISONS, null)
      ?.split(',')
      ?.map { it.trim() }
      ?.toSet()
      ?: emptySet()

    // Process each prison configured for reviews
    // Transactions are performed by the PrisonJobProcessor bean to ensure Spring proxies apply @Transactional
    featureEnabledPrisonCodes.forEach { prisonCode ->
      transactionalPrisonJobProcessor.processForPrison(prisonCode, supplier, consumer)
    }
  },
)
