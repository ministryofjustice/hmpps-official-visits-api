package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService.Companion.getClientAsUser
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import java.time.LocalDateTime

class LocalRequestContextConfigurationTest {
  private val userService: UserService = mock()
  private val interceptor = LocalRequestContextInterceptor(userService)
  private val req = MockHttpServletRequest()
  private val res = MockHttpServletResponse()

  @Test
  fun `should populate request context with prison user`() {
    val user = prisonUser(username = "USER_NAME")
    setSecurityContext(username = user.username)

    whenever(userService.getUser(user.username)) doReturn user

    interceptor.preHandle(req, res, "null")

    val context = req.getAttribute(LocalRequestContext::class.simpleName!!) as LocalRequestContext

    context.user isEqualTo user
    context.requestAt isCloseTo LocalDateTime.now()
  }

  @Test
  fun `should populate request context with client as user`() {
    setSecurityContext(username = null)

    interceptor.preHandle(req, res, "null")

    val context = req.getAttribute(LocalRequestContext::class.simpleName!!) as LocalRequestContext

    context.user isEqualTo getClientAsUser("client-id")
    context.requestAt isCloseTo LocalDateTime.now()
    verify(userService, never()).getUser(any())
  }

  @Test
  fun `should throw AccessDeniedException if username is not found`() {
    setSecurityContext(username = "USER_NAME")

    whenever(userService.getUser("USER_NAME")) doReturn null

    val exception = assertThrows<AccessDeniedException> { interceptor.preHandle(req, res, "null") }

    exception.message isEqualTo "User with username USER_NAME not found"
  }

  @Test
  fun `should throw AccessDeniedException when authentication is null`() {
    SecurityContextHolder.setContext(mock { on { authentication } doReturn null })

    val exception = assertThrows<AccessDeniedException> { interceptor.preHandle(req, res, "null") }

    exception.message isEqualTo "User is not authenticated"
  }

  @AfterEach
  fun afterEach() {
    SecurityContextHolder.clearContext()
  }

  private fun setSecurityContext(username: String?, clientId: String = "client-id") = mock<AuthAwareAuthenticationToken> {
    on { this.userName } doReturn username
    on { this.clientId } doReturn clientId
  }.also { token -> SecurityContextHolder.setContext(mock { on { authentication } doReturn token }) }
}
