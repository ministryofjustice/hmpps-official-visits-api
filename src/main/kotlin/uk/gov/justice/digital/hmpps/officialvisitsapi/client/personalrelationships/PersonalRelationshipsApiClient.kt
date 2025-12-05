package uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PagedModelPrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactSummary

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
      .bodyToMono(PagedModelPrisonerContactSummary::class.java)
      .doOnError { error ->
        log.info(
          "Error fetching approved contacts for $prisonerNumber in personal relationship client",
          error,
        )
      }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
    return pagedModelMono?.content?.toList() ?: emptyList()
  }

  fun getApprovedContacts(prisonerNumber: String): List<PrisonerContactSummary> {
    val pagedModelMono = personalRelationshipsApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisoner/{prisonerNumber}/contact")
          .queryParam("active", true)
          .queryParam("page", 0)
          .queryParam("size", 100)
          .build(prisonerNumber)
      }
      .retrieve()
      .bodyToMono(PagedModelPrisonerContactSummary::class.java)
      .doOnError { error ->
        log.info(
          "Error fetching approved contacts for $prisonerNumber in personal relationship client",
          error,
        )
      }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
    return pagedModelMono?.content?.toList() ?: emptyList()
  }
}
