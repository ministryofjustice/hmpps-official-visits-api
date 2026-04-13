package uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.maybeQueryParam
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ContactDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PagedModelPrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerAndContactId
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactRelationship
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactRelationshipsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCode

@Component
class PersonalRelationshipsApiClient(private val personalRelationshipsApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getContactById(contactId: Long): ContactDetails? = run {
    personalRelationshipsApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/contact/{contactId}")
          .build(contactId)
      }
      .retrieve()
      .bodyToMono<ContactDetails>()
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
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
      .bodyToMono<PagedModelPrisonerContactSummary>()
      .doOnError { error ->
        log.info(
          "Error fetching approved contacts for $prisonerNumber in personal relationship client",
          error,
        )
      }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
    return pagedModelMono?.content ?: emptyList()
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
      .bodyToMono<PagedModelPrisonerContactSummary>()
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

  fun getReferenceDataByGroup(groupCode: String): List<ReferenceCode>? = personalRelationshipsApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/reference-codes/group/{groupCode}")
        .build(groupCode)
    }
    .retrieve()
    .bodyToMono<List<ReferenceCode>>()
    .block()

  fun getPrisonerContactRelationships(prisonerNumber: String, contactId: Long) = personalRelationshipsApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/prisoner/{prisonerNumber}/contact/{contactId}")
        .build(prisonerNumber, contactId)
    }
    .retrieve()
    .bodyToMono<List<PrisonerContactSummary>>()
    .doOnError { error ->
      log.info(
        "Error fetching relationships for contactId $contactId and $prisonerNumber in personal relationships client",
        error,
      )
    }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()

  fun getAllPrisonerContacts(prisonerNumber: String, approved: Boolean? = null, currentTerm: Boolean? = null): List<PrisonerContactSummary> {
    val pagedModelMono = personalRelationshipsApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisoner/{prisonerNumber}/contact")
          .maybeQueryParam("approvedVisitor", approved)
          .queryParam("page", 0)
          .queryParam("size", 200)
          .build(prisonerNumber)
      }
      .retrieve()
      .bodyToMono<PagedModelPrisonerContactSummary>()
      .doOnError { error ->
        log.info(
          "Error fetching contacts for $prisonerNumber in personal relationship client",
          error,
        )
      }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
    return pagedModelMono?.content?.let { contacts ->
      if (currentTerm == null) {
        contacts
      } else {
        contacts.filter { contact -> contact.currentTerm == currentTerm }
      }
    } ?: emptyList()
  }

  fun getPrisonerContactRelationships(prisonerContacts: Set<PrisonerContactDto>): Collection<PrisonerContactRelationship> = run {
    if (prisonerContacts.isEmpty()) {
      return emptyList()
    }

    personalRelationshipsApiWebClient.post()
      .uri("/prisoner-contact/relationships/summary")
      .bodyValue(PrisonerContactRelationshipsRequest(prisonerContacts.map { PrisonerAndContactId(it.prisonerNumber, it.contactId) }))
      .retrieve()
      .bodyToMono<Collection<PrisonerContactRelationship>>()
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block() ?: emptyList()
  }
}

data class PrisonerContactDto(val prisonerNumber: String, val contactId: Long)
