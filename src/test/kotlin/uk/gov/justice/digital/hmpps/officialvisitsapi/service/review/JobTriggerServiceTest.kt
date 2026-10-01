package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.review

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.ExpireVisitReviewsJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.GetReviewCandidates2DayCheckJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.GetReviewCandidates7DayCheckJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobRunner
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.ProcessReviewCandidatesJob

class JobTriggerServiceTest {

  private val jobRunner: JobRunner = mock()
  private val getReviewCandidates7DayCheckJob: GetReviewCandidates7DayCheckJob = mock()
  private val getReviewCandidates2DayCheckJob: GetReviewCandidates2DayCheckJob = mock()
  private val processReviewCandidatesJob: ProcessReviewCandidatesJob = mock()
  private val expireVisitReviewsJob: ExpireVisitReviewsJob = mock()

  private val jobTriggerService: JobTriggerService = JobTriggerService(
    jobRunner,
    getReviewCandidates7DayCheckJob,
    getReviewCandidates2DayCheckJob,
    processReviewCandidatesJob,
    expireVisitReviewsJob,
  )

  @Test
  fun `should run identify candidate visits to check job when job type is IDENTIFY_CANDIDATE_VISITS_TO_CHECK`() {
    jobTriggerService.run(JobType.GET_REVIEW_CANDIDATES_7_DAY_CHECK)
    verify(jobRunner).runJob(getReviewCandidates7DayCheckJob)
  }

  @Test
  fun `should run process candidate visits to check job when job type is PROCESS_CANDIDATE_VISITS_TO_CHECK`() {
    jobTriggerService.run(JobType.PROCESS_REVIEW_CANDIDATES)
    verify(jobRunner).runJob(processReviewCandidatesJob)
  }

  @Test
  fun `should run identify candidate visits to recheck job when job type is IDENTIFY_CANDIDATE_VISITS_TO_RECHECK`() {
    jobTriggerService.run(JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK)
    verify(jobRunner).runJob(getReviewCandidates2DayCheckJob)
  }

  @Test
  fun `should run expire visits for review job when job type is EXPIRE_VISITS_FOR_REVIEW`() {
    jobTriggerService.run(JobType.EXPIRE_VISIT_REVIEWS)
    verify(jobRunner).runJob(expireVisitReviewsJob)
  }
}
