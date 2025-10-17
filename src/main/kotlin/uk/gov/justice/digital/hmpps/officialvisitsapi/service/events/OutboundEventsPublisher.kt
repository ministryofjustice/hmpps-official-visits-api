package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events

interface OutboundEventsPublisher {
  fun send(event: OutboundHMPPSDomainEvent)
}
