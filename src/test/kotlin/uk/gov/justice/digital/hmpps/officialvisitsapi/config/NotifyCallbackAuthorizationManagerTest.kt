package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import java.util.function.Supplier

class NotifyCallbackAuthorizationManagerTest {

  @Test
  fun `should not allow callback when no token configured`() {
    val manager = NotifyCallbackAuthorizationManager("")

    val decision = manager.authorize(authenticationSupplier(), RequestAuthorizationContext(MockHttpServletRequest()))

    assertThat((decision as AuthorizationDecision).isGranted).isFalse()
  }

  @Test
  fun `should allow callback when bearer token matches`() {
    val manager = NotifyCallbackAuthorizationManager("secret-token")
    val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer secret-token") }

    val decision = manager.authorize(authenticationSupplier(), RequestAuthorizationContext(request))

    assertThat((decision as AuthorizationDecision).isGranted).isTrue()
  }

  @Test
  fun `should reject callback when bearer token is missing or invalid`() {
    val manager = NotifyCallbackAuthorizationManager("secret-token")

    assertThrows<BadCredentialsException> {
      manager.authorize(authenticationSupplier(), RequestAuthorizationContext(MockHttpServletRequest()))
    }

    val invalidRequest = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer wrong-token") }
    assertThrows<BadCredentialsException> {
      manager.authorize(authenticationSupplier(), RequestAuthorizationContext(invalidRequest))
    }
  }

  private fun authenticationSupplier(): Supplier<Authentication?> = Supplier { null }
}
