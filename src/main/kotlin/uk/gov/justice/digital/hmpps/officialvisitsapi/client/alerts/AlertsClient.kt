package uk.gov.justice.digital.hmpps.officialvisitsapi.client.alerts

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alertsapi.model.Alert
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alertsapi.model.PageAlert

inline fun <reified T : Any> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class AlertsClient(private val alertsApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerAlerts(prisonerNumber: String): List<Alert> = alertsApiWebClient
    .get()
    .uri("/prisoner/{prisonerNumber}/alerts", prisonerNumber)
    .retrieve()
    .bodyToMono<PageAlert>()
    .doOnError { error -> log.info("Error looking up prisoner alerts by prisoner number $prisonerNumber in prisoner alerts client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?.content?.toList() ?: emptyList()
}
