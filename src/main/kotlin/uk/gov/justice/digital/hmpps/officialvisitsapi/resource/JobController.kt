package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.review.JobTriggerService

/**
 * These endpoints are secured in the ingress rather than the app so that they can be called from
 * within the namespace without requiring authentication
 */

@Tag(name = "Job Controller")
@RestController
@ProtectedByIngress
@RequestMapping(value = ["job-admin"], produces = [MediaType.TEXT_PLAIN_VALUE])
class JobController(private val jobTriggerService: JobTriggerService) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(summary = "Endpoint to trigger a job, perhaps from a cron schedule.")
  @PostMapping(path = ["/run/{jobName}"])
  @ResponseStatus(HttpStatus.OK)
  fun runJob(@PathVariable("jobName") jobName: JobType) = jobTriggerService.run(jobName).resultMessage
}
