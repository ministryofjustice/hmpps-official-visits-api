package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerBookingMovedEvent

@Component
class PrisonerBookingMovedEventHandler : DomainEventHandler<PrisonerBookingMovedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerBookingMovedEvent) {
    val prisoner = event.prisonerNumber()
    val prison = event.prisonCode()
    log.info("PRISONER BOOKING MOVED EVENT:  Prisoner $prisoner with prison $prison booking moved - Not actioned")
  }
}
