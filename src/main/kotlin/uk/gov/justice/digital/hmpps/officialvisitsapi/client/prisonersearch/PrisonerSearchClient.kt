package uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.model.PagePrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.model.Prisoner

inline fun <reified T : Any> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class PrisonerSearchClient(private val prisonerSearchApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisoner(prisonerNumber: String): Prisoner? = prisonerSearchApiWebClient
    .get()
    .uri("/prisoner/{prisonerNumber}", prisonerNumber)
    .retrieve()
    .bodyToMono(Prisoner::class.java)
    .doOnError { error -> log.info("Error looking up prisoner by prisoner number $prisonerNumber in prisoner search client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  fun findByPrisonerNumbers(prisonerNumbers: List<String>, batchSize: Int = 1000): List<Prisoner> {
    if (prisonerNumbers.isEmpty()) return emptyList()

    require(batchSize in 1..1000) {
      "Batch size must be between 1 and 1000"
    }

    return prisonerNumbers.chunked(batchSize).flatMap {
      prisonerSearchApiWebClient.post()
        .uri("/prisoner-search/prisoner-numbers")
        .bodyValue(PrisonerNumbers(it))
        .retrieve()
        .bodyToMono(typeReference<List<Prisoner>>())
        .block() ?: emptyList()
    }
  }

  fun findPrisonersBySearchTerm(prisonCode: String, searchTerm: String) = prisonerSearchApiWebClient
    .get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/prison/{prisonCode}/prisoners")
        .queryParam("term", searchTerm)
        .queryParam("page", 0)
        .queryParam("size", 200)
        .build(prisonCode)
    }
    .retrieve()
    .bodyToMono(typeReference<PagePrisoner>())
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()?.content?.toList() ?: emptyList()

  // TODO: use Prison Register service to find prison name and cache results
  fun findPrisonName(prisonCode: String) = prisonerSearchApiWebClient
    .get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/prison/{prisonCode}/prisoners")
        .queryParam("page", 0)
        .queryParam("size", 1) // Only need one prisoner to get the prison name, so page size of 1 is sufficient and more efficient than retrieving more prisoners
        .build(prisonCode)
    }
    .retrieve()
    .bodyToMono(typeReference<PagePrisoner>())
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()?.content?.firstOrNull()?.prisonName ?: "Unknown prison name"
}

data class PrisonerNumbers(val prisonerNumbers: List<String>)

@Component
class PrisonerValidator(val prisonerSearchClient: PrisonerSearchClient) {
  fun validatePrisonerAtPrison(prisonerNumber: String, prisonCode: String): Prisoner = prisonerSearchClient.getPrisoner(prisonerNumber)?.takeUnless { prisoner -> prisoner.prisonId != prisonCode }
    ?: throw ValidationException("Prisoner $prisonerNumber not found at prison $prisonCode")
}
