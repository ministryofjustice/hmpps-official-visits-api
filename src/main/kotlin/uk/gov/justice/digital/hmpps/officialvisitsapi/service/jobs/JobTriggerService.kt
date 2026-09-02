package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType.EXPIRE_VISITS_FOR_REVIEW
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType.IDENTIFY_CANDIDATE_VISITS_TO_CHECK
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType.PROCESS_CANDIDATE_VISITS_TO_CHECK

@Service
class JobTriggerService(
  private val jobRunner: JobRunner,
  private val identifyCandidateVisitsToCheckJob: IdentifyCandidateVisitsToCheckJob,
  private val processCandidateVisitsToCheckJob: ProcessCandidateVisitsToCheckJob,
  private val expireVisitsForReviewJob: ExpireVisitsForReviewJob,
) {
  fun run(job: JobType) = when (job) {
    IDENTIFY_CANDIDATE_VISITS_TO_CHECK -> jobRunner.runJob(identifyCandidateVisitsToCheckJob)
    PROCESS_CANDIDATE_VISITS_TO_CHECK -> jobRunner.runJob(processCandidateVisitsToCheckJob)
    EXPIRE_VISITS_FOR_REVIEW -> jobRunner.runJob(expireVisitsForReviewJob)
  }
}
