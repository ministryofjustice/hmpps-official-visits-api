package uk.gov.justice.digital.hmpps.officialvisitsapi.exception

data class OffenderErrorResponse(
  val offenderVisitId: Long,
  val dpsOfficialVisitId: Long,
  val message: String,
)
