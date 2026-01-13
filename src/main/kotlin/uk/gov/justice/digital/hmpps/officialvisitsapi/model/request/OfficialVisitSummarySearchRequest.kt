package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.util.UUID

data class OfficialVisitSummarySearchRequest(
  @Schema(description = "The search term can be a prisoner number, name or partial name.", example = "Smith")
  val searchTerm: String? = null,

  @field:NotNull(message = "The start date for search mandatory")
  @Schema(description = "The earliest date the official visits will start", example = "2022-12-23")
  val startDate: LocalDate?,

  @field:NotNull(message = "The end date for search mandatory")
  @Schema(description = "The latest date the official visits will end", example = "2022-12-23")
  val endDate: LocalDate?,

  @Schema(description = "The visit types to search for", examples = ["IN_PERSON", "VIDEO", "TELEPHONE", "UNKNOWN"])
  val visitTypes: List<VisitType>?,

  @Schema(description = "The visit statuses to search for", examples = ["SCHEDULED", "COMPLETED", "CANCELLED", "EXPIRED"])
  val visitStatuses: List<VisitStatusType>?,

  @Schema(description = "The prisoner numbers to search for", examples = ["G9190VP", "G9190VP"])
  val prisonerNumbers: List<String>? = emptyList(),

  @Schema(description = "The location identifiers to search for", examples = ["aaaa-bbbb-9f9f9f9f-9f9f9f9f"])
  val locationIds: List<UUID>?,
)
