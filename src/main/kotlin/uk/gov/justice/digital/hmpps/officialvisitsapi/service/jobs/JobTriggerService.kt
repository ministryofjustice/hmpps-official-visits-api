package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType.IDENTIFY_CANDIDATE_VISITS_TO_CHECK

@Service
class JobTriggerService(
  private val jobRunner: JobRunner,
  private val identifyCandidateVisitsToCheckJob: IdentifyCandidateVisitsToCheckJob,
) {
  fun run(job: JobType) = when (job) {
    IDENTIFY_CANDIDATE_VISITS_TO_CHECK -> jobRunner.runJob(identifyCandidateVisitsToCheckJob)
  }
}
