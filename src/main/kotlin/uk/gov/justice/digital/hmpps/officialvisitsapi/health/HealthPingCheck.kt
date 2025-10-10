@file:Suppress("ktlint:standard:filename")

package uk.gov.justice.digital.hmpps.officialvisitsapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

/**
 * Defines which other services will be health-checked by this one.
 * These are the services that this one depends on to function.
 */

@Component("hmppsAuth")
class HmppsAuthHealthPing(@Qualifier("hmppsAuthHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("locationsInsidePrisonApi")
class LocationsInsidePrisonApiHealthPingCheck(@Qualifier("locationsInsidePrisonApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("manageUsersApi")
class ManageUsersHealthPingCheck(@Qualifier("manageUsersApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("prisonerSearchApi")
class PrisonerSearchApiHealthPingCheck(@Qualifier("prisonerSearchApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)
