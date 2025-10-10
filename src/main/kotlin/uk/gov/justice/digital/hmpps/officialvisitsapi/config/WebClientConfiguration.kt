package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${api.base.url.hmpps-auth}") val hmppsAuthBaseUri: String,
  @param:Value("\${api.base.url.locations-inside-prison}") val locationsInsidePrisonApiBaseUri: String,
  @Value("\${api.base.url.manage-users}") private val manageUsersBaseUri: String,
  @Value("\${api.base.url.prisoner-search}") val prisonerSearchBaseUri: String,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @param:Value("\${api.timeout:20s}") val timeout: Duration,
) {
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun locationsInsidePrisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, "locations-inside-prison", locationsInsidePrisonApiBaseUri, timeout)

  @Bean
  fun locationsInsidePrisonApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(locationsInsidePrisonApiBaseUri, healthTimeout)

  @Bean
  fun manageUsersApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(manageUsersBaseUri, healthTimeout)

  @Bean
  fun manageUsersApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder.authorisedWebClient(authorizedClientManager, "manage-users", manageUsersBaseUri, timeout)

  @Bean
  fun prisonerSearchApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(prisonerSearchBaseUri, healthTimeout)

  @Bean
  fun prisonerSearchApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder.authorisedWebClient(authorizedClientManager, "prisoner-search", prisonerSearchBaseUri, timeout)
}
