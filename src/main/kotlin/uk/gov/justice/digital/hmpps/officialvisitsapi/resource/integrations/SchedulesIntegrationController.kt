package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.integrations

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.OfficialVisitFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses
import java.time.LocalDate
import java.util.UUID

@Tag(name = "Integrations - schedules")
@RestController
@RequestMapping(value = ["integrations/schedules"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class SchedulesIntegrationController(private val facade: OfficialVisitFacade) {

  @Operation(
    summary = "Return a paged list of official visit IDs matching the supplied criteria",
    description = "The date range is mandatory and inclusive. All other filters are optional and combined with AND. Only the matching official visit IDs are returned.",
  )
  @GetMapping("/official-visits/ids")
  @Parameters(
    Parameter(name = "page", `in` = ParameterIn.QUERY, description = "Zero-based page index (0..N)", schema = Schema(type = "integer", defaultValue = "0")),
    Parameter(name = "size", `in` = ParameterIn.QUERY, description = "The size of the page to be returned", schema = Schema(type = "integer", defaultValue = "20")),
    Parameter(
      name = "sort",
      `in` = ParameterIn.QUERY,
      description = "Sort criteria in the format property,(asc|desc). Sortable properties: officialVisitId, visitDate, startTime. Defaults to officialVisitId,asc. officialVisitId is always the final sort key, so results page consistently.",
      array = ArraySchema(schema = Schema(type = "string")),
    ),
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A paginated list of matching official visit IDs",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_PRISONER_SCHEDULES__RO')")
  fun findOfficialVisitIds(
    @RequestParam
    @Parameter(description = "The earliest visit date in ISO format (YYYY-MM-DD)", example = "2026-11-01", required = true)
    fromDate: LocalDate,
    @RequestParam
    @Parameter(description = "The latest visit date in ISO format (YYYY-MM-DD)", example = "2026-11-30", required = true)
    toDate: LocalDate,
    @RequestParam(required = false)
    @Parameter(description = "The prisoner number", example = "A1234AA")
    prisonerNumber: String?,
    @RequestParam(required = false)
    @Parameter(description = "The prison code", example = "MDI")
    prisonCode: String?,
    @RequestParam(required = false)
    @Parameter(description = "The visit status", example = "SCHEDULED")
    visitStatus: VisitStatusType?,
    @RequestParam(required = false)
    @Parameter(description = "The visit type", example = "IN_PERSON")
    visitType: VisitType?,
    @RequestParam(required = false)
    @Parameter(description = "The DPS location ID", example = "9485cf4a-750b-4d74-b594-59bacbcda247")
    locationId: UUID?,
    @Parameter(hidden = true)
    @PageableDefault(size = 20, page = 0, direction = Sort.Direction.ASC, sort = ["officialVisitId"])
    pageable: Pageable,
  ): PagedModel<Long> = facade.searchForOfficialVisitIds(fromDate, toDate, prisonerNumber, prisonCode, visitStatus, visitType, locationId, pageable)
}
