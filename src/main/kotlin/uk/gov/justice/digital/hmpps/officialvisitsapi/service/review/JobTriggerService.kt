package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.review

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.ExpireVisitReviewsJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.GetReviewCandidates2DayCheckJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.GetReviewCandidates7DayCheckJob
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobRunner
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.JobType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs.ProcessReviewCandidatesJob

@Service
class JobTriggerService(
  private val jobRunner: JobRunner,
  private val getReviewCandidates7DayCheckJob: GetReviewCandidates7DayCheckJob,
  private val getReviewCandidates2DayCheckJob: GetReviewCandidates2DayCheckJob,
  private val processReviewCandidatesJob: ProcessReviewCandidatesJob,
  private val expireVisitReviewsJob: ExpireVisitReviewsJob,
) {
  fun run(job: JobType) = when (job) {
    JobType.GET_REVIEW_CANDIDATES_7_DAY_CHECK -> jobRunner.runJob(getReviewCandidates7DayCheckJob)
    JobType.GET_REVIEW_CANDIDATES_2_DAY_CHECK -> jobRunner.runJob(getReviewCandidates2DayCheckJob)
    JobType.PROCESS_REVIEW_CANDIDATES -> jobRunner.runJob(processReviewCandidatesJob)
    JobType.EXPIRE_VISIT_REVIEWS -> jobRunner.runJob(expireVisitReviewsJob)
  }
}
