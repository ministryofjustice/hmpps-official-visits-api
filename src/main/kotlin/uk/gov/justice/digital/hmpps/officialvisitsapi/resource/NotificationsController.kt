package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.ErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.getLocalRequestContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications.NotificationsFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NotificationResponse

@Tag(name = "Notifications")
@RestController
@RequestMapping(value = ["notification"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class NotificationsController(private val notificationFacade: NotificationsFacade) {

  @Operation(summary = "Endpoint to support the sending of notifications for official visits.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The response containing the details of all notifications sent",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = NotificationResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No official visit found to send the notification for.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping(path = ["/{officialVisitId}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__RW')")
  fun sendNotification(
    @PathVariable @Parameter(
      name = "officialVisitId",
      description = "The identifier of the official visit to send the notification for.",
      example = "1",
      required = true,
    ) officialVisitId: Long,
    @Valid
    @RequestBody
    @Parameter(description = "The request containing the details of the notification", required = true)
    request: NotificationRequest,
    httpRequest: HttpServletRequest,
  ) = notificationFacade.sendNotification(officialVisitId, request, httpRequest.getLocalRequestContext().user)
}
