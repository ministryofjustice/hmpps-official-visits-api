package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.ErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications.NotifyCallbackService
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotifyCallbackNotificationRequest

@Tag(name = "Notify callback")
@RestController
@Validated
@RequestMapping(value = ["notify/callback"], produces = [MediaType.APPLICATION_JSON_VALUE])
class NotifyCallbackController(private val notifyCallbackService: NotifyCallbackService) {

  @Operation(
    summary = "Receives callback events from GOV.UK Notify after notification delivery attempts.",
    description = "Accept and process the gov notify callback for notifications",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Gov notify callback processed",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("permitAll()")
  fun callback(
    @Valid @RequestBody request: NotifyCallbackNotificationRequest,
    @RequestHeader(name = "Authorization", required = false) providedSecret: String?,
  ) {
    notifyCallbackService.processCallback(request, providedSecret)
  }
}
