package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound

interface OutboundEventsPublisher {
  fun send(event: OutboundHMPPSDomainEvent)
}
