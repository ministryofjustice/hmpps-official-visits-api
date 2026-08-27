package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import java.time.LocalDate

abstract class DailyJob(
  jobType: JobType,
  private val timeSource: TimeSource,
  private val supplier: (LocalDate) -> Collection<OfficialVisitEntity>,
  private val consumer: (Collection<OfficialVisitEntity>) -> Unit,
) : JobDefinition(
  jobType,
  {
    consumer(supplier(timeSource.today()))
  },
)
