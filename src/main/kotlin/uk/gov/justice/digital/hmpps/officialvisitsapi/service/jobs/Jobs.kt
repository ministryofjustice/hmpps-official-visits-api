package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

enum class JobType(val resultMessage: String) {
  IDENTIFY_CANDIDATE_VISITS_TO_CHECK("Identify candidate visits check job triggered"),
}

abstract class JobDefinition(val jobType: JobType, private val block: () -> Unit) {
  fun runJob() {
    block()
  }
}
