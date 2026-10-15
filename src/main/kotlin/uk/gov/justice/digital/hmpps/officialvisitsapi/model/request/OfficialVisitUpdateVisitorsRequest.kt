package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description = """
  Request to update the visitors on the visit, adding, removing and updating as required.
  
  In the request the following values are meaningful:
    - officialVisitId == 0 - means this is a new visitor to add
    - officialVisitorId > 0 - means this is a pre-existing visitor to update or retain.
  
  Any existing visitors on the visit which are missing from the update request will be removed.
   
  Every visitor requested must be in a relationship with the prisoner, however,
  that relationship may have changed from what is reflected on the visit now. The visitor
  details will be updated to reflect the latest data from the request. For example, if the 
  visitor has changed their name, or their relationship has changed.
  """,
)
data class OfficialVisitUpdateVisitorsRequest(
  val officialVisitors: List<OfficialVisitor>,
)
