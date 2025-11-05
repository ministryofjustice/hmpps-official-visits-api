package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.userDetails
import java.net.URLEncoder

class ManageUsersApiMockServer : MockServer(8093) {

  fun stubGetUserDetails(username: String, authSource: AuthSource = AuthSource.auth, name: String, activeCaseload: String? = null, userId: String = "TEST") {
    stubFor(
      get("/users/${username.urlEncode()}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(userDetails(username, name, authSource, activeCaseload, userId)))
            .withStatus(200),
        ),
    )
  }

  private fun String.urlEncode() = URLEncoder.encode(this, "utf-8")
}

class ManageUsersApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = ManageUsersApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    server.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    server.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    server.stop()
  }
}
