package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin.LocationsFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses

@Tag(name = "Admin")
@RestController
@RequestMapping(value = ["/admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class LocationsController(private val facade: LocationsFacade) {

  @GetMapping(path = ["/prison/{prisonCode}/visit-locations"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Get all visit locations at a prison",
    description = "Requires role: ROLE_OFFICIAL_VISITS_ADMIN.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of visit locations",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitLocation::class))],
      ),
    ],
  )
  fun getAllVisitLocationsAtPrison(
    @Parameter(description = "prison code", required = true)
    @PathVariable prisonCode: String,
  ): List<VisitLocation> = facade.getAllVisitLocationsAtPrison(prisonCode)
}
