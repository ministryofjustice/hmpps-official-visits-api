package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.DomainEvent

/**
 * Event handler implementations should not swallow exceptions. If an exception is thrown it should be propagated.
 * By not catching errors events will (and should) be retried automatically.
 */
fun interface DomainEventHandler<T : DomainEvent<*>> {
  fun handle(event: T)
}
