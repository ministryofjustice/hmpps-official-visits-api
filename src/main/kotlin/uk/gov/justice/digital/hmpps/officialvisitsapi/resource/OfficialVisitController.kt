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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails

@Tag(name = "Official visits")
@RestController
@RequestMapping(value = ["official-visit"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class OfficialVisitController(private val facade: OfficialVisitFacade) {

  @Operation(summary = "Endpoint to support the creation of official visits.")
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
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__RW')")
  fun createOfficialVisit(
    @Valid
    @RequestBody
    @Parameter(description = "The request with the official visit details", required = true)
    request: CreateOfficialVisitRequest,
    httpRequest: HttpServletRequest,
  ): CreateOfficialVisitResponse = facade.createOfficialVisit(request, httpRequest.getLocalRequestContext().user)

  @GetMapping("/{officialVisitId}")
  @Operation(
    summary = "Get Official visit",
    description = "Gets a Official visit by  id",
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
        description = "No Official visit found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS_RW')")
  fun getOfficialVisits(
    @PathVariable("officialVisitId") @Parameter(
      name = "officialVisitId",
      description = "The id of the Official visit",
      example = "123456",
    ) officialVisitId: Long,
  ): ResponseEntity<Any> {
    val officialVisit = facade.getOfficialVisitById(officialVisitId)
    return ResponseEntity.ok(officialVisit)
  }
}
