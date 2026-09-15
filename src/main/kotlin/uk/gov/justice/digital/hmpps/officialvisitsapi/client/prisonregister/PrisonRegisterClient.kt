package uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregister

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.ContactDetailsDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.PrisonDto

@Component
class PrisonRegisterClient(private val prisonRegisterApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonDetails(prisonId: String) = prisonRegisterApiWebClient
    .get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/prisons/id/{prisonId}")
        .build(prisonId)
    }
    .retrieve()
    .bodyToMono<PrisonDto>()
    .doOnError { error -> log.info("Error lookup prison for $prisonId in prison register client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  fun getPrisonContactDetails(prisonId: String, department: String) = prisonRegisterApiWebClient
    .get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/secure/prisons/id/{prisonId}/department/contact-details")
        .queryParam("departmentType", department)
        .build(prisonId)
    }
    .retrieve()
    .bodyToMono<ContactDetailsDto>()
    .doOnError { error -> log.info("Error lookup contact for $prisonId dept $department in prison register client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()
}
