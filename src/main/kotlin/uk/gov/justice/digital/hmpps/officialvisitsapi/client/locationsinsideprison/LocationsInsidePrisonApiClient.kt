package uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
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

  fun getLocationByKey(key: String): Location? = locationsInsidePrisonApiWebClient.get()
    .uri("/locations/key/{key}", key)
    .retrieve()
    .bodyToMono(Location::class.java)
    .doOnError { error -> log.info("Error looking up location by location key $key in locations inside prison client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  fun getLocationsByKeys(keys: Set<String>): List<Location> = locationsInsidePrisonApiWebClient.post()
    .uri("/locations/keys")
    .bodyValue(keys)
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .doOnError { error -> log.info("Error looking up locations by location keys $keys in locations inside prison client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()

  fun getNonResidentialAppointmentLocationsAtPrison(prisonCode: String) = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/prison/{prisonCode}/non-residential-usage-type/APPOINTMENT")
        .queryParam("sortByLocalName", true)
        .queryParam("formatLocalName", true)
        .build(prisonCode)
    }
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .doOnError { error -> log.info("Error looking up non-residential appointment locations by prison code $prisonCode in locations inside prison client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()

  fun getVideoLinkLocationsAtPrison(prisonCode: String): List<Location> = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/prison/{prisonCode}/location-type/VIDEO_LINK")
        .queryParam("sortByLocalName", true)
        .queryParam("formatLocalName", true)
        .build(prisonCode)
    }
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .doOnError { error -> log.info("Error looking up video link locations by prison code $prisonCode in locations inside prison client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()?.filter(Location::leafLevel) ?: emptyList()
}

@Component
class LocationValidator(private val locationsInsidePrisonClient: LocationsInsidePrisonClient) {

  fun validatePrisonLocation(prisonCode: String, locationKey: String): Location {
    val location = locationsInsidePrisonClient.getLocationByKey(locationKey)
    validate(prisonCode, setOf(locationKey), listOfNotNull(location))
    return location!!
  }

  fun validatePrisonLocations(prisonCode: String, locationKeys: Set<String>): List<Location> {
    val locations = locationsInsidePrisonClient.getLocationsByKeys(locationKeys)
    validate(prisonCode, locationKeys, locations)
    return locations
  }

  private fun validate(prisonCode: String, locationKeys: Set<String>, maybeLocations: List<Location>) {
    val maybeFoundLocations = maybeLocations.associateBy { it.key }

    if (maybeFoundLocations.isEmpty()) {
      validationError("The following ${if (locationKeys.size == 1) "location was" else "locations were"} not found $locationKeys")
    }

    val mayBeMissingLocations = maybeFoundLocations.keys.filterNot { locationKeys.contains(it) }

    if (mayBeMissingLocations.isNotEmpty()) {
      validationError("The following ${if (mayBeMissingLocations.size == 1) "location was" else "locations were"} not found $mayBeMissingLocations")
    }

    val maybeLocationsAtDifferentPrison = maybeFoundLocations.values.filterNot { it.isAtPrison(prisonCode) }.map { it.key }

    if (maybeLocationsAtDifferentPrison.isNotEmpty()) {
      validationError("The following ${if (maybeLocationsAtDifferentPrison.size == 1) "location is" else "locations are"} not at prison code $prisonCode $maybeLocationsAtDifferentPrison")
    }

    val maybeInactiveLocations = maybeFoundLocations.values.filterNot { it.isActive() }.map { it.key }

    if (maybeInactiveLocations.isNotEmpty()) {
      validationError("The following ${if (maybeInactiveLocations.size == 1) "location is" else "locations are"} not active at prison code $prisonCode $maybeInactiveLocations")
    }
  }

  private fun validationError(message: String): Unit = throw ValidationException(message)
}

fun Location.isActive() = active

fun Location.isAtPrison(prisonCode: String) = prisonId == prisonCode
