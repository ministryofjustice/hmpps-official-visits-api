package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

data class OfficialVisitUpdateVisitorsRequest(
  val officialVisitors: List<OfficialVisitor>,
)
