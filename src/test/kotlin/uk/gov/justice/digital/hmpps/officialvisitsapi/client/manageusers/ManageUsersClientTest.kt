package uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.userCaseloads
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.userDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock.ManageUsersApiMockServer

class ManageUsersClientTest {
  private val server = ManageUsersApiMockServer().also { it.start() }
  private val client = ManageUsersClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get users details`() {
    server.stubGetUserDetails("username", name = "name")

    client.getUsersDetails("username") isEqualTo userDetails("username", "name")
  }

  @Test
  fun `should get users caseloads`() {
    server.stubGetUserCaseloads("username")

    client.getUserCaseloads("username") isEqualTo userCaseloads("username", true)
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}
