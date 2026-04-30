package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.admin

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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin.EmailNotificationFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.SendTestEmailRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Admin")
@RestController
@RequestMapping(value = ["/admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class EmailNotificationController(private val facade: EmailNotificationFacade) {

  @PostMapping(path = ["/notifications/email/test"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Send a test email notification",
    description = "Requires role: ROLE_OFFICIAL_VISITS_ADMIN.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Email request accepted"),
      ApiResponse(
        responseCode = "400",
        description = "The request was invalid",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN')")
  fun sendTestEmail(@Valid @RequestBody request: SendTestEmailRequest) {
    facade.sendTestEmail(request)
  }
}
