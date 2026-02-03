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
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Synchronisation")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class TimeSlotSyncController(val syncFacade: SyncFacade) {
  @GetMapping(path = ["/time-slot/{prisonTimeSlotId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the data for a prison time slot by ID",
    description = """
      Requires role: OFFICIAL_VISITS_MIGRATION.
      Used to get the details for one prison time slot.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The prison time slot matching the ID provided in the request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = SyncTimeSlot::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prison time slot with this ID was found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION')")
  fun syncGetTimeSlotById(
    @Parameter(description = "The internal ID for a prison time slot", required = true)
    @PathVariable prisonTimeSlotId: Long,
  ): SyncTimeSlot = syncFacade.getTimeSlotById(prisonTimeSlotId)

  @PostMapping(path = ["/time-slot"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new prison time slot for official visits",
    description = """
      Requires role: OFFICIAL_VISITS_MIGRATION.
      Used to create a new prison time slot for official visits.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created a prison time slot for official visits",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncTimeSlot::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request was invalid",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION')")
  fun syncCreateTimeSlot(
    @Valid @RequestBody request: SyncCreateTimeSlotRequest,
  ): SyncTimeSlot = syncFacade.createTimeSlot(request)

  @PutMapping(path = ["/time-slot/{prisonTimeSlotId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates a prison time slot",
    description = """
      Requires role: OFFICIAL_VISITS_MIGRATION.
      Used to update a a prison time slot for official visits.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated a prison time slot",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncTimeSlot::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison time slot ID was not found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request was invalid",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION')")
  fun syncUpdateTimeSlot(
    @Parameter(description = "The internal ID for the prison time slot", required = true)
    @PathVariable prisonTimeSlotId: Long,
    @Valid @RequestBody request: SyncUpdateTimeSlotRequest,
  ) = syncFacade.updateTimeSlot(prisonTimeSlotId, request)

  @DeleteMapping("/time-slot/{timeSlotId}")
  @Operation(
    summary = "Delete a prison time slot",
    description = "Delete a time slot if there are no visit slots associated with it.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Deleted the time slot",
      ),
      ApiResponse(
        responseCode = "409",
        description = "The prison time slot had visit slots associated with it and cannot be deleted.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION')")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  open fun syncDeleteTimeSlot(@PathVariable timeSlotId: Long) = syncFacade.deleteTimeSlot(timeSlotId)
}
