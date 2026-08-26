package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import java.time.LocalDate

abstract class DailyJob(
  jobType: JobType,
  private val timeSource: TimeSource,
  private val bookingsSupplier: (LocalDate) -> Collection<OfficialVisitEntity>,
  private val bookingsConsumer: (Collection<OfficialVisitEntity>) -> Unit,
) : JobDefinition(
  jobType,
  {
    bookingsConsumer(bookingsSupplier(timeSource.today()))
  },
)
