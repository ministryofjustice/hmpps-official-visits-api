package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.http.Fault
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.userCaseloads
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.userDetails
import java.net.URLEncoder

class ManageUsersApiMockServer : MockServer(8093) {

  fun stubGetUserDetails(
    username: String,
    authSource: AuthSource = AuthSource.auth,
    name: String,
    activeCaseload: String? = null,
    userId: String = "TEST",
  ) {
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

  fun stubGetUserCaseloads(
    username: String,
    active: Boolean = true,
    activeCaseload: PrisonCaseload = PrisonCaseload(id = BIRMINGHAM, name = "Birmingham"),
    caseloads: List<PrisonCaseload> = listOf(PrisonCaseload(id = BIRMINGHAM, name = "Birmingham")),
  ) {
    stubFor(
      get("/prisonusers/${username.urlEncode()}/caseloads")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(userCaseloads(username, active, activeCaseload, caseloads)))
            .withStatus(200),
        ),
    )
  }

  private fun String.urlEncode() = URLEncoder.encode(this, "utf-8")

  fun stubUserFault(username: String) {
    stubFor(
      get("/users/${username.urlEncode()}")
        .willReturn(
          aResponse()
            .withFault(Fault.CONNECTION_RESET_BY_PEER),
        ),
    )
  }
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
