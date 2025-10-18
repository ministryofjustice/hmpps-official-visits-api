package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerReceivedEvent

@Component
class PrisonerReceivedEventHandler : DomainEventHandler<PrisonerReceivedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: PrisonerReceivedEvent) {
    val prisoner = event.prisonerNumber()
    val prison = event.prisonCode()
    log.info("PRISONER RECEIVED EVENT: Prisoner $prisoner received at $prison - No action taken")
  }
}
