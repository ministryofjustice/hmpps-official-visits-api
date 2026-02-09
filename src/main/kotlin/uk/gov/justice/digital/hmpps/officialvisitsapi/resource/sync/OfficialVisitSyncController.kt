package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.sync.SyncFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Synchronisation")
@RestController
@RequestMapping(value = ["sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class OfficialVisitSyncController(private val syncFacade: SyncFacade) {

  @GetMapping(value = ["/official-visit/id/{officialVisitId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(summary = "Endpoint to return one official visit by ID")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Official visit details",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncOfficialVisit::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION', 'OFFICIAL_VISITS_ADMIN')")
  fun getOfficialVisitById(
    @PathVariable(required = true) officialVisitId: Long,
  ): SyncOfficialVisit = syncFacade.getOfficialVisitById(officialVisitId)

  @PostMapping(path = ["/official-visit"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates an official visit via synchronisation from NOMIS",
    description = """
      Requires role: OFFICIAL_VISITS_MIGRATION.
      Used to create an official visit in DPS as a result of synchronisation from NOMIS.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully created the official visit",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncOfficialVisit::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison visit slot did not exist",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request was invalid",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION')")
  fun syncCreateVisitSlot(
    @Valid @RequestBody request: SyncCreateOfficialVisitRequest,
  ): SyncOfficialVisit = syncFacade.createOfficialVisit(request)

  @DeleteMapping("/official-visit/id/{officialVisitId}")
  @Operation(
    summary = "Delete a official visit by ID",
    description = """
      Delete a official visit and associated child entities like official visitor, prisoner visitors and equipments .
      This endpoint is idempotent so if the official visit does not exist it will silently succeed.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Deleted the official visit by ID",
      ),
      ApiResponse(
        responseCode = "409",
        description = "The official visit and associated official visitors and prison visitor cannot be deleted.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION', 'OFFICIAL_VISITS_ADMIN')")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  fun syncDeleteOfficialVisit(
    @PathVariable(required = true) officialVisitId: Long,
  ) = syncFacade.deleteOfficialVisit(officialVisitId)
}
