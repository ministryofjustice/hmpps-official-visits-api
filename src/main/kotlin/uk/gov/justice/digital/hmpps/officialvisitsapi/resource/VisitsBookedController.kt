package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitBookedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.VisitBookedService

@Tag(name = "Visits booked")
@RestController
@RequestMapping(value = ["visits-booked"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class VisitsBookedController(private val visitBookedService: VisitBookedService) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(summary = "Endpoint to return the official visits booked for a prison - TRIAL ONLY")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of visits booked at the prison",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = VisitBookedEntity::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/prison/{prisonCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS__RW')")
  fun getVisitsBookedForPrison(
    @Parameter(description = "The prison code", required = true, example = "MDI")
    @PathVariable("prisonCode", required = true)
    prisonCode: String,
  ): List<VisitBookedEntity> {
    logger.info("Received request for visits booked for prison code $prisonCode")
    return visitBookedService.getVisitsBookedForPrison(prisonCode)
  }
}
