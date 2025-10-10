package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.isEmail
import java.util.*

@Service
class UserService(private val manageUsersClient: ManageUsersClient) {

  companion object {

    private enum class ServiceName {
      OFFICIAL_VISITS_SERVICE,
    }

    private val serviceUser = ServiceUser(
      ServiceName.OFFICIAL_VISITS_SERVICE.name,
      ServiceName.OFFICIAL_VISITS_SERVICE.name,
    )

    fun getServiceAsUser() = serviceUser

    fun getClientAsUser(clientId: String) = ServiceUser(username = clientId, name = clientId)
  }

  fun getUser(username: String): User? = manageUsersClient.getUsersDetails(username)?.let { userDetails ->
    when (userDetails.authSource) {
      AuthSource.nomis -> {
        PrisonUser(
          username = username,
          name = userDetails.name,
          email = if (username.isEmail()) username.lowercase() else manageUsersClient.getUsersEmail(username)?.email?.lowercase(),
          activeCaseLoadId = userDetails.activeCaseLoadId,
        )
      }

      else -> throw AccessDeniedException("Users with auth source ${userDetails.authSource} are not supported by this service")
    }
  }
}

abstract class User(
  val username: String,
  val name: String,
) {
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

class PrisonUser(
  val email: String? = null,
  val activeCaseLoadId: String? = null,
  username: String,
  name: String,
) : User(username, name) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as PrisonUser

    if (email != other.email) return false
    if (activeCaseLoadId != other.activeCaseLoadId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (email?.hashCode() ?: 0)
    result = 31 * result + (activeCaseLoadId?.hashCode() ?: 0)
    return result
  }
}

class ServiceUser(username: String, name: String) : User(username, name)
