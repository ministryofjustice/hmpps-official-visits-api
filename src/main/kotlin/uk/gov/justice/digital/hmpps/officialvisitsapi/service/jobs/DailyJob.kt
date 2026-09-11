package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import java.time.LocalDate

abstract class DailyJob<T>(
  jobType: JobType,
  private val timeSource: TimeSource,
  private val supplier: (LocalDate) -> Collection<T>,
  private val consumer: (Collection<T>) -> Unit,
) : JobDefinition(
  jobType,
  {
    consumer(supplier(timeSource.today()))
  },
)
