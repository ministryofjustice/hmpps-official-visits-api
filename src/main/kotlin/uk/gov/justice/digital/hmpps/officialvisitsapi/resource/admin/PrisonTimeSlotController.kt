package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.getLocalRequestContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin.PrisonTimeSlotFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotResponse
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Time slots")
@RestController
@RequestMapping(value = ["/admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonTimeSlotController(val facade: PrisonTimeSlotFacade) {

  @PostMapping(path = ["/time-slot"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new prison time slot for official visits",
    description = """
      Requires role: ROLE_OFFICIAL_VISIT_ADMIN.
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
            schema = Schema(implementation = TimeSlotResponse::class),
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
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISIT_ADMIN')")
  fun createTimeSlot(
    @Valid @RequestBody request: CreateTimeSlotRequest,
    httpRequest: HttpServletRequest,
  ): TimeSlotResponse = facade.createPrisonTimeSlot(request, httpRequest.getLocalRequestContext().user)

  @PutMapping(path = ["/time-slot/{prisonTimeSlotId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates a prison time slot",
    description = """
      Requires role: ROLE_OFFICIAL_VISIT_ADMIN.
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
            schema = Schema(implementation = TimeSlotResponse::class),
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
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISIT_ADMIN')")
  fun updateTimeSlot(
    @Parameter(description = "The internal ID for the prison time slot", required = true)
    @PathVariable prisonTimeSlotId: Long,
    @Valid @RequestBody request: UpdateTimeSlotRequest,
    httpRequest: HttpServletRequest,
  ) = facade.updatePrisonTimeSlot(prisonTimeSlotId, request, httpRequest.getLocalRequestContext().user)

  @DeleteMapping("/time-slot/{timeSlotId}")
  @Operation(
    summary = "Delete a prison time slot",
    description = """
      Delete a time slot if there are no visit slots associated with it.
      """,
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
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISIT_ADMIN')")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  fun deleteTimeSlot(
    @PathVariable timeSlotId: Long,
    httpRequest: HttpServletRequest,
  ) = facade.deletePrisonTimeSlot(
    timeSlotId,
    httpRequest.getLocalRequestContext().user,
  )
}
