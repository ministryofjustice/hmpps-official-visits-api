package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.REQUEST_TIMEOUT
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import tools.jackson.databind.exc.InvalidFormatException
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.DownstreamServiceException
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.DuplicateOffenderVisitIdConflictException
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.DuplicateOffenderVisitIdErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.CaseloadAccessException
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.CaseloadAccessHiddenException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class OfficialVisitsApiExceptionHandler {
  @ExceptionHandler(IllegalArgumentException::class)
  fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = e.message,
        developerMessage = e.message,
      ),
    ).also { log.info("Illegal argument exception: {}", e.message) }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = e.message,
        developerMessage = "Validation failure: ${e.message}",
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Entity not found exception: {}", e.message) }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "Forbidden: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.debug("Forbidden (403) returned: {}", e.message) }

  @ExceptionHandler(BadCredentialsException::class)
  fun handleBadCredentialsException(e: BadCredentialsException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(UNAUTHORIZED)
    .body(
      ErrorResponse(
        status = UNAUTHORIZED,
        userMessage = "Unauthorized: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.debug("Unauthorized (401) returned: {}", e.message) }

  @ExceptionHandler(EntityInUseException::class)
  fun handleEntityInUseException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(CONFLICT)
    .body(
      ErrorResponse(
        status = CONFLICT,
        userMessage = "${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Entity in use exception", e) }

  @ExceptionHandler(CaseloadAccessException::class)
  fun handleCaseLoadAccessException(e: CaseloadAccessException): ResponseEntity<ErrorResponse> {
    log.info("Case load access exception: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT.value(),
          userMessage = e.message,
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(CaseloadAccessHiddenException::class)
  fun handleCaseLoadAccessHiddenException(e: CaseloadAccessHiddenException): ResponseEntity<ErrorResponse> {
    log.info("Case load access hidden exception: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND.value(),
          userMessage = e.message,
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(DuplicateOffenderVisitIdConflictException::class)
  fun handleOffenderVisitIdConflictException(e: DuplicateOffenderVisitIdConflictException) = ResponseEntity
    .status(CONFLICT)
    .body(
      DuplicateOffenderVisitIdErrorResponse(
        e.offenderVisitId,
        e.officialVisitId,
        e.message,
      ),
    )

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    val cause = e.cause
    val message = if (cause is InvalidFormatException && cause.targetType.isEnum) {
      val field = cause.path.joinToString(".") { it.propertyName ?: "[${it.index}]" }
      val allowed = cause.targetType.enumConstants.joinToString(", ")
      "Validation failed: `$field` must be one of: $allowed"
    } else {
      "Validation failure: Couldn't read request body"
    }

    return ResponseEntity.status(BAD_REQUEST).body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = message,
        developerMessage = message,
      ),
    ).also { log.error(e.message, e) }
  }

  @ExceptionHandler(DownstreamServiceException::class)
  fun handleDownstreamServiceException(e: DownstreamServiceException) = ResponseEntity
    .status(REQUEST_TIMEOUT)
    .body(
      ErrorResponse(
        status = REQUEST_TIMEOUT.value(),
        userMessage = e.message,
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
    val message = "The request contained invalid values or types. Refer to the API documentation for details of valid requests."
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = message,
          developerMessage = message,
        ),
      ).also { log.error("Unexpected exception", e) }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
