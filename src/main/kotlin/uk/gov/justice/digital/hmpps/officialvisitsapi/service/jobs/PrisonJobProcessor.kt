package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import java.time.LocalDate

/**
 * Helper bean that performs per-prison processing inside a transactional boundary.
 * This is a separate bean so that @Transactional will be applied by Spring proxies.
 */
@Component
class PrisonJobProcessor {
  @Transactional
  fun <T> processForPrison(
    prisonCode: String,
    timeSource: TimeSource,
    supplier: (LocalDate, String) -> Collection<T>,
    consumer: (Collection<T>, String) -> Unit,
  ) {
    val data = supplier(timeSource.today(), prisonCode)
    if (data.isNotEmpty()) {
      consumer(data, prisonCode)
    }
  }
}
