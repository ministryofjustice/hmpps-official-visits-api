package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.ErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.RepairPrisonerVisitsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.RepairPrisonerVisitsResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.migrate.RepairPrisonerVisitsService

@Tag(name = "Repair prisoner visits")
@RestController
@RequestMapping(value = ["repair"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class RepairPrisonerVisitsController(val repairPrisonerVisitsService: RepairPrisonerVisitsService) {

  @PostMapping("prisoner-visits/{prisonerNumber}", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Replace all official visits for a single prisoner with the data from NOMIS",
    description = "This replaces all official visits for one prisoner with their visits as they sexist in NOMIS",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of the visit and visitor ID mappings",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = RepairPrisonerVisitsResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request failed validation with invalid or missing data",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION', 'OFFICIAL_VISITS_ADMIN')")
  fun repairPrisonerVisits(
    @PathVariable(required = true) prisonerNumber: String,
    @Valid @RequestBody request: RepairPrisonerVisitsRequest,
  ): RepairPrisonerVisitsResponse = repairPrisonerVisitsService.repairPrisonerVisits(prisonerNumber, request)
}
