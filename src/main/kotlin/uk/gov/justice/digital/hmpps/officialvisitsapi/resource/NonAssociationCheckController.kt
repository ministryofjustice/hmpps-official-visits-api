package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NonAssociationCheckRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NonAssociationVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.NonAssociationsService

@Tag(name = "Non-associations")
@RestController
@RequestMapping(value = ["official-visit/non-association-check"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class NonAssociationCheckController(private val nonAssociationsService: NonAssociationsService) {

  @Operation(
    summary = "Endpoint to check whether a prisoner's non-associates already have an official visit.",
    description = "Returns the official visits of any of the prisoner's open non-associations that are scheduled at the same prison on the same date as the visit being checked. Returns an empty list when the prisoner has no open non-associations, or when none of them have a visit on that date.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The official visits of any non-associates on the given date, empty if there are none",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = NonAssociationVisitResponse::class)),
          ),
        ],
      ),
    ],
  )
  @PostMapping(path = ["/prison/{prisonCode}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__RW')")
  fun checkForNonAssociationVisits(
    @PathVariable @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MDI",
      required = true,
    ) prisonCode: String,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the details of the visit to check", required = true)
    request: NonAssociationCheckRequest,
  ): List<NonAssociationVisitResponse> = nonAssociationsService.checkForNonAssociationVisits(prisonCode, request)
}
