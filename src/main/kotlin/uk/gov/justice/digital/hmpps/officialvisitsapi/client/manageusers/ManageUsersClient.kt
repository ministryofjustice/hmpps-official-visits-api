package uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto

@Component
class ManageUsersClient(private val manageUsersApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getUsersDetails(username: String): UserDetailsDto? = manageUsersApiWebClient
    .get()
    .uri("/users/{username}", username)
    .retrieve()
    .bodyToMono(UserDetailsDto::class.java)
    .doOnError { error -> log.info("Error looking up user details by username $username in manage users client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()
}
