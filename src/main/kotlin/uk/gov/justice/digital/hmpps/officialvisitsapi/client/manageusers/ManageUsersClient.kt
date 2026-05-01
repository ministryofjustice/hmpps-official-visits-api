package uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto
import java.time.Duration

@Component
class ManageUsersClient(private val manageUsersApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * This will attempt to find the user details for this username.
   * If it receives a 404 Not Found response it will return a null immediately without retrying.
   * It will wait a maximum of 5 seconds for a response on each call
   * If an exception is encountered e.g. timeout, or connection error, it will retry upto 3 further times.
   * Each retry will double the previous back-off time, so after 250ms, 500ms and 1000ms
   * If it still fails it will return null and swallow any exceptions, not propagate them.
   */
  // TODO: Look into different caching options - currently the user details are looking up per request
  // TODO: but there are issues when a user switches their caseload (the cached copy keeps the old active caseload)
  // TODO: And activeCaseload is deprecated in the response here....  rethink this area.
  // @Cacheable(CacheConfiguration.USER_DETAILS_BY_USERNAME_CACHE)
  fun getUsersDetails(username: String): UserDetailsDto? = manageUsersApiWebClient
    .get()
    .uri("/users/{username}", username)
    .retrieve()
    .bodyToMono<UserDetailsDto>()
    .timeout(Duration.ofSeconds(5))
    .retryWhen(Retry.backoff(3, Duration.ofMillis(250)).filter { it !is WebClientResponseException.NotFound })
    .doOnError { error -> log.info("Error looking up user details by username $username in manage users client", error) }
    .onErrorResume { Mono.empty() }
    .block()
}
