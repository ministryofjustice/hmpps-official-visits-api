package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications.NotifyCallbackService
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotifyCallbackRequest

@Tag(name = "Notify callback")
@RestController
@RequestMapping(value = ["notify/callback"], produces = [MediaType.APPLICATION_JSON_VALUE])
class NotifyCallbackController(private val notifyCallbackService: NotifyCallbackService) {

  @Operation(summary = "Receives callback events from GOV.UK Notify after notification delivery attempts.")
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("permitAll()")
  fun callback(
    @Valid @RequestBody request: NotifyCallbackRequest,
    @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorizationHeader: String?,
  ) {
    notifyCallbackService.processCallback(request, authorizationHeader)
  }
}
