package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.sync.SyncFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncVisitSlot
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Synchronisation")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class VisitSlotSyncController(val syncFacade: SyncFacade) {

  @GetMapping(path = ["/visit-slot/{prisonVisitSlotId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the data for a prison visit slot by ID",
    description = """
      Requires role: OFFICIAL_VISITS_MIGRATION.
      Used to get the details for one prison visit slot.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The prison visit slot matching the ID provided in the request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = SyncVisitSlot::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prison visit slot with this ID was found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION')")
  fun syncGetVisitSlotById(
    @Parameter(description = "The internal ID for a prison visit slot", required = true)
    @PathVariable prisonVisitSlotId: Long,
  ): SyncVisitSlot = syncFacade.getVisitSlotById(prisonVisitSlotId)

  @PostMapping(path = ["/visit-slot"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new prison visit slot for official visits",
    description = """
      Requires role: OFFICIAL_VISITS_MIGRATION.
      Used to create a new prison visit slot for official visits.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created a prison visit slot for official visits",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncVisitSlot::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request was invalid or had missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION')")
  fun syncCreateVisitSlot(
    @Valid @RequestBody request: SyncCreateVisitSlotRequest,
  ): SyncVisitSlot = syncFacade.createVisitSlot(request)

  @PutMapping(path = ["/visit-slot/{prisonVisitSlotId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates a prison visit slot with new or altered details",
    description = """
      Requires role: OFFICIAL_VISITS_MIGRATION.
      Used to update a a prison visit slot for official visits.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated a prison visit slot",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncVisitSlot::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison visit slot ID was not found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data supplied in the request",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION')")
  fun syncUpdateVisitSlot(
    @Parameter(description = "The internal ID for the prison visit slot", required = true)
    @PathVariable prisonVisitSlotId: Long,
    @Valid @RequestBody request: SyncUpdateVisitSlotRequest,
  ) = syncFacade.updateVisitSlot(prisonVisitSlotId, request)

  @DeleteMapping("{visitSlotId}")
  @Operation(
    summary = "Delete prisoner Visit slot",
    description = "Delete the visit slot. Only allowed if there are no official visits associated with it.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Deleted the visit slot  successfully",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the prisoner visit slot ",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "The visit has attached entities such as official visit and cannot be deleted.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole(OFFICIAL_VISITS_MIGRATION)")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  open fun syncDelete(@PathVariable visitSlotId: Long) = syncFacade.deleteVisitSlot(visitSlotId)
}
