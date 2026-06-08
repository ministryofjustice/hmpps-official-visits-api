package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications.NotificationFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitNotification

@Tag(name = "Official visit notifications")
@RestController
@RequestMapping(value = ["official-visit"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class OfficialVisitNotificationsController(
  private val notificationFacade: NotificationFacade,
) {

  @Operation(summary = "Get all notifications sent for an official visit ID.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of notification rows for the official visit",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = OfficialVisitNotification::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(path = ["/id/{officialVisitId}/notifications"])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS_RW')")
  fun getNotificationsByOfficialVisitId(
    @PathVariable @Parameter(
      name = "officialVisitId",
      description = "The official visit identifier",
      example = "1",
      required = true,
    ) officialVisitId: Long,
  ): List<OfficialVisitNotification> = notificationFacade.getNotificationsByOfficialVisitId(officialVisitId)
}
