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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.getLocalRequestContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin.VisitSlotFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Admin")
@RestController
@RequestMapping(value = ["/admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class VisitSlotController(private val facade: VisitSlotFacade) {

  @PostMapping(path = ["/time-slot/{prisonTimeSlotId}/visit-slot"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Create a visit slot in an existing prison time slot",
    description = """
      Requires role: ROLE_OFFICIAL_VISIT_ADMIN.
      Creates a new visit slot for the given prison time slot.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created a prison visit slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitSlot::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request was invalid",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(responseCode = "404", description = "Prison time slot not found"),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISIT_ADMIN')")
  fun createVisitSlot(
    @Parameter(description = "The internal ID for prison time slot", required = true)
    @PathVariable prisonTimeSlotId: Long,
    @Valid @RequestBody request: CreateVisitSlotRequest,
    httpRequest: HttpServletRequest,
  ): VisitSlot = facade.createVisitSlot(prisonTimeSlotId, request, httpRequest.getLocalRequestContext().user)

  @PutMapping(path = ["/visit-slot/id/{visitSlotId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Update capacities for a visit slot",
    description = """
      Requires role: ROLE_OFFICIAL_VISIT_ADMIN.
      Only capacities (max groups, adults, and video) may be updated.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated a prison visit slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitSlot::class))],
      ),
      ApiResponse(responseCode = "404", description = "No prison visit slot with this ID was found"),
      ApiResponse(responseCode = "400", description = "The request was invalid"),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISIT_ADMIN')")
  fun updateVisitSlot(
    @Parameter(description = "The internal ID for the prison visit slot", required = true)
    @PathVariable visitSlotId: Long,
    @Valid @RequestBody request: UpdateVisitSlotRequest,
    httpRequest: HttpServletRequest,
  ): VisitSlot = facade.updateVisitSlot(visitSlotId, request, httpRequest.getLocalRequestContext().user)

  @DeleteMapping("/visit-slot/id/{visitSlotId}")
  @Operation(
    summary = "Delete a prison visit slot",
    description = """
      Requires role: ROLE_OFFICIAL_VISIT_ADMIN.
      Delete a visit slot if there are no official visits associated with it.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Deleted the visit slot"),
      ApiResponse(
        responseCode = "409",
        description = "The prison visit slot had visits associated so cannot be deleted",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the visit slot ",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISIT_ADMIN')")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  fun deleteVisitSlot(
    @Parameter(description = "The internal ID for the prison visit slot", required = true)
    @PathVariable visitSlotId: Long,
    httpRequest: HttpServletRequest,
  ) = facade.deleteVisitSlot(visitSlotId, httpRequest.getLocalRequestContext().user)
}
