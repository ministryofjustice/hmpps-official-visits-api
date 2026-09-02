package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.review

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.ExpireVisitsForReviewJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.IdentifyCandidateVisitsToCheckJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobRunner
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.ProcessCandidateVisitsToCheckJob

class JobTriggerServiceTest {

  private val jobRunner: JobRunner = mock()
  private val identifyCandidateVisitsToCheckJob: IdentifyCandidateVisitsToCheckJob = mock()
  private val processCandidateVisitsToCheckJob: ProcessCandidateVisitsToCheckJob = mock()
  private val expireVisitsForReviewJob: ExpireVisitsForReviewJob = mock()
  private val jobTriggerService: JobTriggerService = JobTriggerService(jobRunner, identifyCandidateVisitsToCheckJob, processCandidateVisitsToCheckJob, expireVisitsForReviewJob)

  @Test
  fun `should run identify candidate visits to check job when job type is IDENTIFY_CANDIDATE_VISITS_TO_CHECK`() {
    jobTriggerService.run(JobType.IDENTIFY_CANDIDATE_VISITS_TO_CHECK)
    verify(jobRunner).runJob(identifyCandidateVisitsToCheckJob)
  }

  @Test
  fun `should run process candidate visits to check job when job type is PROCESS_CANDIDATE_VISITS_TO_CHECK`() {
    jobTriggerService.run(JobType.PROCESS_CANDIDATE_VISITS_TO_CHECK)
    verify(jobRunner).runJob(processCandidateVisitsToCheckJob)
  }

  @Test
  fun `should run expire visits for review job when job type is EXPIRE_VISITS_FOR_REVIEW`() {
    jobTriggerService.run(JobType.EXPIRE_VISITS_FOR_REVIEW)
    verify(jobRunner).runJob(expireVisitsForReviewJob)
  }
}
