package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.userDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService.Companion.getClientAsUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService.Companion.getServiceAsUser

class UserServiceTest {

  private val manageUsersClient: ManageUsersClient = mock()
  private val userService = UserService(manageUsersClient)

  @Test
  fun `getServiceAsUser should return ContactDetails for valid email username`() {
    with(getServiceAsUser()) {
      username isEqualTo "OFFICIAL_VISITS_SERVICE"
      name isEqualTo "OFFICIAL_VISITS_SERVICE"
    }
  }

  @Test
  fun `getClientAsUser should return ContactDetails for valid email username`() {
    with(getClientAsUser("client")) {
      username isEqualTo "client"
      name isEqualTo "client"
    }
  }

  @Test
  fun `getUser should return prison user when authSource is nomis`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails("testUser", "Test User", authSource = AuthSource.nomis, activeCaseLoadId = BIRMINGHAM)

    userService.getUser("testUser") as PrisonUser isEqualTo PrisonUser(username = "testUser", name = "Test User", activeCaseLoadId = BIRMINGHAM)
  }

  @Test
  fun `getUser should throw AccessDeniedException for unsupported authSource`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails("testUser", "Test User", authSource = AuthSource.delius)

    val exception = assertThrows<AccessDeniedException> { userService.getUser("testUser") }

    exception.message isEqualTo "Users with auth source delius are not supported by this service"
  }

  @Test
  fun `getUser should return null when the user is not found`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn null

    userService.getUser("testUser") isEqualTo null
  }

  @Test
  fun `should create prison users`() {
    PrisonUser(username = "username", name = "name")
    PrisonUser(username = "username", name = "name", activeCaseLoadId = BIRMINGHAM)
  }
}
