package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component

/**
 * Helper component that performs per-prison processing.
 */
@Component
class PrisonProcessor {
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
