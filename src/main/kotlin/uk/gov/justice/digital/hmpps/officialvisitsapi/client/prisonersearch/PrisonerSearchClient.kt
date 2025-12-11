package uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDate

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
}

@Component
class PrisonerValidator(val prisonerSearchClient: PrisonerSearchClient) {
  fun validatePrisonerAtPrison(prisonerNumber: String, prisonCode: String): Prisoner = prisonerSearchClient.getPrisoner(prisonerNumber)?.takeUnless { prisoner -> prisoner.prisonId != prisonCode }
    ?: throw ValidationException("Prisoner $prisonerNumber not found at prison $prisonCode")
}

// TODO: Tim generate the code from openApi
// Ideally this model would be generated and not hard coded, however at time of writing the Open API generator did not
// play nicely with the JSON api spec for this service
data class Prisoner(
  val prisonerNumber: String,
  val prisonId: String? = null,
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val bookingId: String? = null,
  val lastPrisonId: String? = null,
  val cellLocation: String? = null,
  val middleNames: String? = null,
  val offenderBookId: String? = null,
  val locationDescription: String? = null,
  val prisonName: String? = null,
)
