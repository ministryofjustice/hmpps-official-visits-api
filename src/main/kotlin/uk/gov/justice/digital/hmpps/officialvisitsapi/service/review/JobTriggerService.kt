package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.review

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.ExpireVisitsForReviewJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.IdentifyCandidateVisitsToCheckJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobRunner
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.ProcessCandidateVisitsToCheckJob

@Service
class JobTriggerService(
  private val jobRunner: JobRunner,
  private val identifyCandidateVisitsToCheckJob: IdentifyCandidateVisitsToCheckJob,
  private val processCandidateVisitsToCheckJob: ProcessCandidateVisitsToCheckJob,
  private val expireVisitsForReviewJob: ExpireVisitsForReviewJob,
) {
  fun run(job: JobType) = when (job) {
    JobType.IDENTIFY_CANDIDATE_VISITS_TO_CHECK -> jobRunner.runJob(identifyCandidateVisitsToCheckJob)
    JobType.PROCESS_CANDIDATE_VISITS_TO_CHECK -> jobRunner.runJob(processCandidateVisitsToCheckJob)
    JobType.EXPIRE_VISITS_FOR_REVIEW -> jobRunner.runJob(expireVisitsForReviewJob)
  }
}
