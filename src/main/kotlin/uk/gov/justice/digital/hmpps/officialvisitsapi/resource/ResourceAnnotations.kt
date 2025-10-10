package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@ApiResponses(
  value = [
    ApiResponse(
      responseCode = "401",
      description = "Unauthorised, requires a valid Oauth2 token",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ErrorResponse::class),
        ),
      ],
    ),
    ApiResponse(
      responseCode = "403",
      description = "Forbidden, requires an appropriate role",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ErrorResponse::class),
        ),
      ],
    ),
  ],
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class AuthApiResponses

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ProtectedByIngress
