package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.ErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.getLocalRequestContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.OfficialVisitFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCompletionRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitSummarySearchResponse

@Tag(name = "Official visits")
@RestController
@RequestMapping(value = ["official-visit"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class OfficialVisitController(private val facade: OfficialVisitFacade) {

  @Operation(summary = "Endpoint to support the creation of official visits.")
  @CaseloadConflictResponse
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The response containing the details of the created official visit",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateOfficialVisitResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(path = ["/prison/{prisonCode}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__RW')")
  fun createOfficialVisit(
    @PathVariable("prisonCode") @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MDI",
      required = true,
    ) prisonCode: String,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the official visit details", required = true)
    request: CreateOfficialVisitRequest,
    httpRequest: HttpServletRequest,
  ): CreateOfficialVisitResponse = facade.createOfficialVisit(prisonCode, request, httpRequest.getLocalRequestContext().user)

  @GetMapping("/prison/{prisonCode}/id/{officialVisitId}")
  @Operation(
    summary = "Get an official visit by prison code and ID",
    description = "Get the full details of an official visit, its visitors and prisoner details",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Official visit found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = OfficialVisitDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No official visit found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS_RW')")
  fun getOfficialVisitByPrisonCodeAndId(
    @PathVariable("prisonCode") @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MIC",
      required = true,
    ) prisonCode: String,
    @PathVariable("officialVisitId") @Parameter(
      name = "officialVisitId",
      description = "The official visit ID",
      example = "123456",
      required = true,
    ) officialVisitId: Long,
  ): OfficialVisitDetails = facade.getOfficialVisitByPrisonCodeAndId(prisonCode, officialVisitId)

  @Operation(summary = "Endpoint to search for official visit summaries for given search criteria.")
  @PostMapping(path = ["/prison/{prisonCode}/find-by-criteria"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS_RW')")
  fun findByCriteria(
    @PathVariable("prisonCode") @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MDI",
      required = true,
    ) prisonCode: String,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the official visit summary search details", required = true)
    request: OfficialVisitSummarySearchRequest,
    @Parameter(
      description = "Zero-based page index (0..N)",
      name = "page",
      schema = Schema(type = "integer", defaultValue = "0"),
    )
    page: Int = 0,
    @Parameter(
      description = "The size of the page to be returned",
      name = "size",
      schema = Schema(type = "integer", defaultValue = "10"),
    )
    size: Int = 20,
  ): PagedModel<OfficialVisitSummarySearchResponse> = facade.searchForOfficialVisitSummaries(prisonCode, request, page, size)

  @Operation(summary = "Completes an official visit.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Official visit completed successfully",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No official visit found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping(path = ["/prison/{prisonCode}/id/{officialVisitId}/complete"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS_RW')")
  fun complete(
    @PathVariable("prisonCode") @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MDI",
      required = true,
    ) prisonCode: String,
    @PathVariable("officialVisitId") @Parameter(
      name = "officialVisitId",
      description = "The official visit identifier",
      example = "123",
      required = true,
    ) officialVisitId: Long,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the official visit completion details", required = true)
    request: OfficialVisitCompletionRequest,
    httpRequest: HttpServletRequest,
  ) {
    facade.completeOfficialVisit(prisonCode, officialVisitId, request, httpRequest.getLocalRequestContext().user)
  }

  @Operation(summary = "Cancels an official visit.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Official visit cancelled successfully",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No official visit found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping(path = ["/prison/{prisonCode}/id/{officialVisitId}/cancel"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS_RW')")
  fun cancel(
    @PathVariable("prisonCode") @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MDI",
      required = true,
    ) prisonCode: String,
    @PathVariable("officialVisitId") @Parameter(
      name = "officialVisitId",
      description = "The official visit identifier",
      example = "123",
      required = true,
    ) officialVisitId: Long,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the official visit cancellation details", required = true)
    request: OfficialVisitCancellationRequest,
    httpRequest: HttpServletRequest,
  ) {
    facade.cancelOfficialVisit(prisonCode, officialVisitId, request, httpRequest.getLocalRequestContext().user)
  }
}
