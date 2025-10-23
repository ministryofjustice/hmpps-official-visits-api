package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.migrate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.ErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitConfigRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.MigrateVisitConfigResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.migrate.MigrationService

@Tag(name = "Migration")
@RestController
@RequestMapping(value = ["migrate"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class MigrateVisitsController(val migrationService: MigrationService) {

  @PostMapping("/visit-configuration", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Migrate official visits configuration",
    description = "Migrate a visit time slot and its associated visit slots, locations and capacity limits",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The time slot and associated visit slots was migrated successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = MigrateVisitConfigResponse::class),
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
  fun migrateVisitConfiguration(
    @Valid @RequestBody request: MigrateVisitConfigRequest,
  ) = migrationService.migrateVisitConfiguration(request)
}
