package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.ErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.getLocalRequestContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.OfficialVisitFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitsForReviewCountResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitsForReviewResponse

@Tag(name = "Visit review")
@RestController
@RequestMapping(value = ["visit-review"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class VisitReviewController(private val facade: OfficialVisitFacade) {

  @GetMapping("/prison/{prisonCode}/count")
  @Operation(
    summary = "Get the count of visits for review at a prison",
    description = "Returns the number of scheduled, future or current, unacknowledged and unexpired visits for review.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Visits for review count",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = VisitsForReviewCountResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "User does not have access to caseload",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS_RW')")
  fun countVisitsForReview(
    @PathVariable @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MDI",
      required = true,
    ) prisonCode: String,
    httpRequest: HttpServletRequest,
  ): VisitsForReviewCountResponse = facade.countVisitsForReview(prisonCode, httpRequest.getLocalRequestContext().user)

  @GetMapping("/prison/{prisonCode}/list")
  @Operation(
    summary = "Get visits for review at a prison",
    description = "Returns scheduled, future or current, unacknowledged and unexpired visits for review with their current issues.",
  )
  @PageableAsQueryParam
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Visits for review",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = VisitsForReviewResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "User does not have access to caseload",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS_RW')")
  fun getVisitsForReview(
    @PathVariable @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MDI",
      required = true,
    ) prisonCode: String,
    @Parameter(hidden = true)
    @PageableDefault(size = 10, page = 0, direction = Sort.Direction.ASC, sort = ["visitDate"])
    pageable: Pageable,
    httpRequest: HttpServletRequest,
  ): PagedModel<VisitsForReviewResponse> = facade.getVisitsForReview(prisonCode, httpRequest.getLocalRequestContext().user, pageable)

  @PutMapping("/prison/{prisonCode}/id/{visitReviewId}/acknowledge")
  @Operation(
    summary = "Acknowledge a visit review",
    description = "Acknowledges all currently unacknowledged and unexpired visit review details for the given visit review. Username is taken from the client token.",
  )
  @CaseloadConflictResponse
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "User does not have access to caseload",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__RW', 'ROLE_OFFICIAL_VISITS_RW')")
  fun acknowledgeVisitReview(
    @PathVariable @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MDI",
      required = true,
    ) prisonCode: String,
    @PathVariable @Parameter(
      name = "visitReviewId",
      description = "The visit review ID",
      example = "123",
      required = true,
    ) visitReviewId: Long,
    httpRequest: HttpServletRequest,
  ) {
    facade.acknowledgeVisitReview(prisonCode, visitReviewId, httpRequest.getLocalRequestContext().user)
  }
}
