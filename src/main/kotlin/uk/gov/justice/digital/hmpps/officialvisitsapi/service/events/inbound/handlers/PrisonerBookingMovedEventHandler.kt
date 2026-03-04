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
    val bookingId = event.bookingId()
    log.info("PRISONER BOOKING MOVED EVENT:  Prisoner $prisoner for prison $prison with booking ID $bookingId moved - Not actioned")
  }
}
