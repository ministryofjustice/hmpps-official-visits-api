package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

data class CreateOfficialVisitResponse(
  val officialVisitId: Long,
  val prisonerNumber: String,
  val visitorAndContactIds: List<Pair<Long, Long?>>,
)
