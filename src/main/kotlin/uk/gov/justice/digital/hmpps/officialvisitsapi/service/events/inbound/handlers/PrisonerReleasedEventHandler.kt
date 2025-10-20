package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerReleasedEvent

@Component
class PrisonerReleasedEventHandler : DomainEventHandler<PrisonerReleasedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: PrisonerReleasedEvent) {
    val prisoner = event.prisonerNumber()
    val prison = event.prisonId()
    when {
      event.isTemporary() -> log.info("RELEASE EVENT HANDLER: Temporary release event - $prisoner from $prison")
      event.isTransferred() || event.isPermanent() -> log.info("RELEASE EVENT HANDLER: Permanent release event $prisoner from $prison")
      else -> log.warn("RELEASE EVENT HANDLER: Ignoring unknown release event $event")
    }
  }
}
