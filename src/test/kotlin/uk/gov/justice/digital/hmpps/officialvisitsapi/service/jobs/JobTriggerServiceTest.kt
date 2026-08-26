package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class JobTriggerServiceTest {

  private val jobRunner: JobRunner = mock()
  private val identifyCandidateVisitsToCheckJob: IdentifyCandidateVisitsToCheckJob = mock()
  private val jobTriggerService: JobTriggerService = JobTriggerService(jobRunner, identifyCandidateVisitsToCheckJob)

  @Test
  fun `should run identify candidate visits to check job when job type is IDENTIFY_CANDIDATE_VISITS_TO_CHECK`() {
    jobTriggerService.run(JobType.IDENTIFY_CANDIDATE_VISITS_TO_CHECK)
    verify(jobRunner).runJob(identifyCandidateVisitsToCheckJob)
  }
}
