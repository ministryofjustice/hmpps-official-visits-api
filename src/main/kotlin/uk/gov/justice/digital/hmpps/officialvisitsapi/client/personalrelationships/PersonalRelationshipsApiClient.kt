package uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationship.model.PagedModelPrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationship.model.PrisonerContactSummary

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class PersonalRelationshipsApiClient(private val personalRelationshipsApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getApprovedContacts(prisonerNumber: String, relationshipType: String): List<PrisonerContactSummary> {
    val pagedModelMono = personalRelationshipsApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisoner/{prisonerNumber}/contact")
          .queryParam("active", true)
          .queryParam("relationshipType", relationshipType)
          .queryParam("page", 0)
          .queryParam("size", 100)
          .build(prisonerNumber)
      }
      .retrieve()
      .bodyToMono(typeReference<PagedModelPrisonerContactSummary>())
      .doOnError { error ->
        log.info(
          "Error  While fetching approved contacts for  $prisonerNumber in Personal relationship client",
          error,
        )
      }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
    return pagedModelMono?.content?.toList() ?: emptyList()
  }
}
