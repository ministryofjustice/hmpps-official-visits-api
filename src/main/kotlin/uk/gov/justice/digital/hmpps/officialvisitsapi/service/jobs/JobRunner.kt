package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.StandardTelemetryEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.TelemetryService
import kotlin.system.measureTimeMillis

@Component
class JobRunner(private val telemetryService: TelemetryService) {

  fun runJob(jobDefinition: JobDefinition): JobType {
    run(jobDefinition)
    return jobDefinition.jobType
  }

  private fun run(jobDefinition: JobDefinition) {
    var timeElapsed = 0L

    runCatching { timeElapsed = measureTimeMillis { jobDefinition.runJob() } }
      .onSuccess { telemetryService.track(JobSuccessStandardTelemetryEvent(jobType = jobDefinition.jobType, timeElapsed = timeElapsed)) }
      .onFailure { exception ->
        run {
          telemetryService.track(JobFailureStandardTelemetryEvent(jobType = jobDefinition.jobType, message = exception.message, timeElapsed = timeElapsed))
          throw exception
        }
      }
  }
}

class JobFailureStandardTelemetryEvent(
  private val jobType: JobType,
  private val message: String? = null,
  private val timeElapsed: Long,
) : StandardTelemetryEvent("official-visits-job-failure") {
  override fun properties(): Map<String, String> = mapOf(
    "jobType" to jobType.name,
    "message" to "".plus(message),
    "timeElapsed" to timeElapsed.toString().plus("ms"),
  )
}

class JobSuccessStandardTelemetryEvent(
  private val jobType: JobType,
  private val timeElapsed: Long,
) : StandardTelemetryEvent("official-visits-job-success") {
  override fun properties(): Map<String, String> = mapOf(
    "jobType" to jobType.name,
    "timeElapsed" to timeElapsed.toString().plus("ms"),
  )
}
