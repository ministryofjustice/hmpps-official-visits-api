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
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.ErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.getLocalRequestContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationSearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NotificationResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.SentNotification
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitChangeStatusResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.NotificationsService

@Tag(name = "Notifications")
@RestController
@RequestMapping(value = ["notification"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class NotificationsController(
  private val notificationsService: NotificationsService,
) {

  @Operation(summary = "Send notifications for an official visit")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The response containing the details of notifications sent",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = NotificationResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The official visit was not found",
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
  ) = notificationsService.sendNotification(officialVisitId, request, httpRequest.getLocalRequestContext().user)

  @Operation(summary = "Retrieve a list of sent notifications with search parameters and pagination support.")
  @PostMapping(path = ["/prison/{prisonCode}/sent-notifications"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS_RW')")
  fun searchSentNotifications(
    @PathVariable @Parameter(
      name = "prisonCode",
      description = "The prison code",
      example = "MDI",
      required = true,
    ) prisonCode: String,
    @Valid
    @RequestBody
    @Parameter(description = "Notification search request", required = true)
    request: NotificationSearchRequest,
    @Parameter(
      description = "Zero-based page index (0..n)",
      name = "page",
      schema = Schema(type = "integer", defaultValue = "0"),
    )
    page: Int = 0,
    @Parameter(
      description = "The size of the page to be returned",
      name = "size",
      schema = Schema(type = "integer", defaultValue = "10"),
    )
    size: Int = 20,
    httpRequest: HttpServletRequest,
  ): PagedModel<SentNotification> = notificationsService.searchSentNotifications(prisonCode, request, page, size, httpRequest.getLocalRequestContext().user)

  @Operation(summary = "Check if a visit has changed significantly since the last notification was sent")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the visit changed status true if the visit has changed significantly since the last notification, otherwise false",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = VisitChangeStatusResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The official visit was not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping(path = ["/{officialVisitId}/change-status"])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS_RW')")
  fun getVisitChangedSinceLastNotification(
    @PathVariable @Parameter(
      name = "officialVisitId",
      description = "The identifier of the official visit to check",
      example = "1",
      required = true,
    ) officialVisitId: Long,
  ): VisitChangeStatusResponse = notificationsService.checkVisitChangedSinceLastNotification(officialVisitId)
}
