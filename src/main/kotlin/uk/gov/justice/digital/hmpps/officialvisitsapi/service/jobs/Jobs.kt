package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

enum class JobType(val resultMessage: String) {
  GET_REVIEW_CANDIDATES_7_DAY_CHECK("Get review candidates 7 day check triggered"),
  GET_REVIEW_CANDIDATES_2_DAY_CHECK("Get review candidates 2 day check triggered"),
  PROCESS_REVIEW_CANDIDATES("Process review candidates triggered"),
  EXPIRE_VISIT_REVIEWS("Expire visit reviews triggered"),
}

abstract class JobDefinition(val jobType: JobType, private val block: () -> Unit) {
  fun runJob() {
    block()
  }
}
