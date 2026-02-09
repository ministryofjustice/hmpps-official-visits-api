package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.AvailableSlotService
import java.time.LocalDate

@Tag(name = "Available slots")
@RestController
@RequestMapping(value = ["available-slots"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class AvailableSlotController(private val availableSlotService: AvailableSlotService) {
  @Operation(summary = "Endpoint to return the available slots for official visits for a prison for a given date range.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of available slots at the prison",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = AvailableSlot::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/{prisonCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS__RW')")
  fun getAvailableSlotsForPrison(
    @PathVariable(required = true) @Parameter(description = "The prison code", required = true, example = "MDI")
    prisonCode: String,
    @Parameter(description = "The from date in ISO format (YYYY-MM-DD).")
    @RequestParam(name = "fromDate", required = true)
    fromDate: LocalDate,
    @Parameter(description = "The to date in ISO format (YYYY-MM-DD).")
    @RequestParam(name = "toDate", required = true)
    toDate: LocalDate,
    @Parameter(description = "Boolean flag. A value of 'true' will only return video reservable slots.")
    @RequestParam(name = "videoOnly", required = false)
    videoOnly: Boolean = false,
  ): List<AvailableSlot> = availableSlotService.getAvailableSlotsForPrison(prisonCode, fromDate, toDate, videoOnly)
}
