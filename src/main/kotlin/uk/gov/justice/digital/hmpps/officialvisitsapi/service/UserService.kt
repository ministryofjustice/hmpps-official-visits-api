package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import java.util.Objects

@Service
class UserService(private val manageUsersClient: ManageUsersClient) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private enum class ServiceName {
      OFFICIAL_VISITS_SERVICE,
    }

    fun getServiceAsUser() = ServiceUser(ServiceName.OFFICIAL_VISITS_SERVICE.name, ServiceName.OFFICIAL_VISITS_SERVICE.name)

    fun getClientAsUser(clientId: String) = ServiceUser(username = clientId, name = clientId)
  }

  fun getUser(username: String): User? {
    val userDetailsDto = manageUsersClient.getUsersDetails(username) ?: return null

    if (!userDetailsDto.active) {
      throw AccessDeniedException("Inactive user account $username")
    }

    if (userDetailsDto.authSource != AuthSource.nomis) {
      throw AccessDeniedException("User $username with auth source ${userDetailsDto.authSource} is not supported by this service")
    }

    val userCaseloads = manageUsersClient.getUserCaseloads(username) ?: return null

    return PrisonUser(
      username = username,
      name = userDetailsDto.name,
      caseloads = userCaseloads.caseloads.map { caseload -> caseload.id.trim().uppercase() },
    )
  }
}

/**
 * The abstract User class that is subclassed by PrisonUser and ServiceUser
 */
abstract class User(val username: String, val name: String) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as User

    if (username != other.username) return false
    if (name != other.name) return false

    return true
  }

  override fun hashCode() = Objects.hash(username, name)
}

/**
 * A real prison staff member with a username, full name and a list of caseloads (prison codes) they have access to.
 */
class PrisonUser(username: String, name: String, val caseloads: List<String>) : User(username, name) {

  fun hasCaseloadAccess(prisonCode: String) = prisonCode.trim().uppercase() in caseloads

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as PrisonUser
    return caseloads == other.caseloads
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = (((31 * result + caseloads.hashCode())))
    return result
  }
}

/**
 * The user is a service user, with no human involved. The UI or API has not provided a reference to an actual person.
 * Caseload checks are not necessary for service users.
 */
class ServiceUser(username: String, name: String) : User(username, name)
