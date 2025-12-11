package uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.CacheConfiguration
import java.util.UUID

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class LocationsInsidePrisonClient(private val locationsInsidePrisonApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getLocationById(id: UUID): Location? = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/{id}")
        .queryParam("formatLocalName", true)
        .build(id)
    }
    .retrieve()
    .bodyToMono(Location::class.java)
    .doOnError { error -> log.info("Error looking up location by location id $id in locations inside prison client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  @Cacheable(CacheConfiguration.OFFICIAL_VISIT_LOCATIONS_BY_PRISON_CACHE)
  fun getOfficialVisitLocationsAtPrison(prisonCode: String, serviceType: String = "OFFICIAL_VISITS"): List<Location> = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/non-residential/prison/{prisonCode}/service/{serviceType}")
        .queryParam("sortByLocalName", true)
        .queryParam("formatLocalName", true)
        .queryParam("filterParents", true)
        .build(prisonCode, serviceType)
    }
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .doOnError { error -> log.info("Error looking up non-residential appointment locations by prison code $prisonCode in locations inside prison client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()
}
