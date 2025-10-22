package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.web.SortDefault
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ReferenceDataItem
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ReferenceDataService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Reference data")
@RestController
@RequestMapping(value = ["reference-data"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ReferenceDataController(private val referenceDataService: ReferenceDataService) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(
    summary = "Endpoint to return reference data for a provided group key. " +
      "Sorted by display order then description by default.",
    parameters = [
      Parameter(
        name = "sort",
        `in` = ParameterIn.QUERY,
        description = "Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.",
        required = false,
        example = "displayOrder,asc",
        array = io.swagger.v3.oas.annotations.media.ArraySchema(schema = Schema(type = "string")),
      ),
    ],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of reference data codes/values",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ReferenceDataItem::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/group/{groupCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS__RW')")
  fun getReferenceDataByGroup(
    @Parameter(description = "The group code of the reference codes to load", required = true, example = "VIS_STS")
    @PathVariable("groupCode", required = true)
    groupCode: ReferenceDataGroup,
    @Parameter(hidden = true, required = false) // Hide from OpenAPI
    @SortDefault("displaySequence", "description")
    sort: Sort,
    @Parameter(description = "Whether to only return active codes or not, defaults to true", required = false)
    @RequestParam(name = "activeOnly", required = false, defaultValue = "true")
    activeOnly: Boolean,
  ): List<ReferenceDataItem> {
    logger.info("Received request for reference data: $groupCode")
    return referenceDataService.getReferenceDataByGroup(groupCode, sort, activeOnly)
  }
}
