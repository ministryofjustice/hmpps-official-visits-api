package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Helper component that performs per-prison processing inside a transactional boundary.
 * This is a separate component so that @Transactional will be applied by Spring proxies.
 */
@Component
class TransactionalPrisonJobProcessor {
  @Transactional
  fun <T> processForPrison(
    prisonCode: String,
    supplier: (String) -> Collection<T>,
    consumer: (Collection<T>, String) -> Unit,
  ) {
    val data = supplier(prisonCode)
    if (data.isNotEmpty()) {
      consumer(data, prisonCode)
    }
  }
}
