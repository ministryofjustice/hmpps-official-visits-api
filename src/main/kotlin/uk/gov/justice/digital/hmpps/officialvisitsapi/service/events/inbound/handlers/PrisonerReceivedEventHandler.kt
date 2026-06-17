package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerReceivedEvent

@Component
class PrisonerReceivedEventHandler(
  private val currentTermComponent: CurrentTermComponent,
) : DomainEventHandler<PrisonerReceivedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerReceivedEvent) {
    val prisonerNumber = event.prisonerNumber()
    val prisonCode = event.prisonCode()
    val reason = event.reason()

    if (event.indicatesANewBooking()) {
      log.info("PRISONER RECEIVED EVENT: Processing event for [$prisonCode] [$prisonerNumber] [reason [$reason] indicates a new booking")
      currentTermComponent.processCurrentTermMarkers(prisonerNumber, "PRISONER RECEIVED EVENT")
    } else {
      log.info("PRISONER RECEIVED EVENT: Ignoring event for [$prisonCode] [$prisonerNumber] [reason [${event.reason()}] does not indicate a new booking")
    }
  }
}
