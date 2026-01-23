package uk.gov.justice.digital.hmpps.officialvisitsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.AdditionalInformation
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.PersonReference

class StubOutboundEventsPublisher(private val receivedEvents: MutableList<OutboundHMPPSDomainEvent> = mutableListOf()) : OutboundEventsPublisher {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun send(event: OutboundHMPPSDomainEvent) {
    receivedEvents.add(event)
    logger.info("Stubbed sending event ($event)")
  }

  fun reset() {
    receivedEvents.clear()
  }

  fun assertHasEvent(event: OutboundEvent, additionalInfo: AdditionalInformation, personReference: PersonReference?) {
    assertThat(receivedEvents)
      .extracting(OutboundHMPPSDomainEvent::eventType, OutboundHMPPSDomainEvent::additionalInformation, OutboundHMPPSDomainEvent::personReference)
      .contains(tuple(event.eventType, additionalInfo, personReference))
  }

  fun assertHasEvent(event: OutboundEvent, additionalInfo: AdditionalInformation) {
    assertThat(receivedEvents)
      .extracting(OutboundHMPPSDomainEvent::eventType, OutboundHMPPSDomainEvent::additionalInformation)
      .contains(tuple(event.eventType, additionalInfo))
  }

  fun assertHasNoEvents(event: OutboundEvent, additionalInfo: AdditionalInformation) {
    assertThat(receivedEvents)
      .extracting(OutboundHMPPSDomainEvent::eventType, OutboundHMPPSDomainEvent::additionalInformation)
      .doesNotContain(tuple(event.eventType, additionalInfo))
  }

  fun assertHasNoEvents(event: OutboundEvent) {
    assertThat(receivedEvents)
      .extracting(OutboundHMPPSDomainEvent::eventType)
      .doesNotContain(tuple(event.eventType))
  }
}
