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
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.userCaseloads
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
    whenever(manageUsersClient.getUserCaseloads("testUser")) doReturn userCaseloads("testUser")

    userService.getUser("testUser") as PrisonUser isEqualTo PrisonUser(username = "testUser", name = "Test User", caseloads = listOf(BIRMINGHAM))
  }

  @Test
  fun `getUser should throw AccessDeniedException for unsupported authSource`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails("testUser", "Test User", authSource = AuthSource.delius)

    val exception = assertThrows<AccessDeniedException> { userService.getUser("testUser") }

    exception.message isEqualTo "User testUser with auth source delius is not supported by this service"
  }

  @Test
  fun `getUser should return null when the user is not found`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn null

    userService.getUser("testUser") isEqualTo null
  }

  @Test
  fun `getUser should throw AccessDeniedException when the user account is not active`() {
    whenever(manageUsersClient.getUsersDetails("testUser")) doReturn userDetails("testUser", "Test User", authSource = AuthSource.nomis, active = false, activeCaseLoadId = BIRMINGHAM)

    val exception = assertThrows<AccessDeniedException> { userService.getUser("testUser") }

    exception.message isEqualTo "Inactive user account testUser"
  }

  @Test
  fun `should create prison users`() {
    val user1 = PrisonUser(username = "username", name = "name", caseloads = emptyList())
    val user2 = PrisonUser(username = "username", name = "name", caseloads = listOf(BIRMINGHAM, MOORLAND, PENTONVILLE))
    user1.caseloads hasSize 0
    user2.caseloads hasSize 3
  }
}
