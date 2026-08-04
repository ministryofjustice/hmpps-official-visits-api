package uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.model.PrisonerNonAssociations

@Component
class NonAssociationsApiClient(private val nonAssociationsApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Returns the open (in effect) non-associations for the given prisoner, or null if the prisoner is not known to the
   * non-associations service.
   */
  fun getPrisonerNonAssociations(prisonerNumber: String): PrisonerNonAssociations? = nonAssociationsApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/prisoner/{prisonerNumber}/non-associations")
        .queryParam("includeOpen", true)
        .queryParam("includeClosed", false)
        .queryParam("includeOtherPrisons", false)
        .build(prisonerNumber)
    }
    .retrieve()
    .bodyToMono<PrisonerNonAssociations>()
    .doOnError { error -> log.info("Error looking up non-associations by prisoner number $prisonerNumber in non-associations client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()
}
