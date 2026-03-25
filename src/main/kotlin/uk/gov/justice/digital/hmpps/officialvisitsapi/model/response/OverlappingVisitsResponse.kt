package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class OverlappingVisitsResponse(
  @Schema(description = "The prisoner number from the initial criteria request")
  val prisonerNumber: String,

  @Schema(description = "The unique identifiers of any scheduled official visits that overlap with the prisoner, empty if there are none")
  val overlappingPrisonerVisits: List<Long>,

  @Schema(description = "The contacts from the initial request criteria, with any overlapping visits if there are any")
  val contacts: List<OverlappingContact>,
)

data class OverlappingContact(
  @Schema(description = "The unique identifier for the prisoner contact from the initial criteria request")
  val contactId: Long,

  @Schema(description = "The unique identifiers of any scheduled official visits that overlap with the contact, empty if there are none")
  val overlappingContactVisits: List<Long>,
)
