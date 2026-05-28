package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.authorization.AuthorizationResult
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.function.Supplier

@Service
class NotifyCallbackAuthorizationManager(
  @Value("\${notify.callback.bearer-token:}") private val govNotifyAccessToken: String,
) : AuthorizationManager<RequestAuthorizationContext> {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun authorize(
    authentication: Supplier<out Authentication?>,
    requestAuthorizationContext: RequestAuthorizationContext,
  ): AuthorizationResult {
    if (govNotifyAccessToken.isBlank()) {
      LOG.info("GOV.UK Notify callback authorization disabled because no token is configured")
      return AuthorizationDecision(true)
    }

    val providedToken = requestAuthorizationContext.request.getHeader("Authorization")?.removePrefix("Bearer ")

    if (providedToken == null || !isTokenValid(providedToken)) {
      LOG.error("Received callback with null or invalid token")
      throw BadCredentialsException("Invalid callback token")
    }

    return AuthorizationDecision(true)
  }

  private fun isTokenValid(providedToken: String): Boolean {
    // Using MessageDigest to mitigate against timed attacks and other potential attack vectors
    return MessageDigest.isEqual(providedToken.toByteArray(), govNotifyAccessToken.toByteArray())
  }
}
