package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerMovedEvent

@Component
class PrisonerMovedEventHandler : DomainEventHandler<PrisonerMovedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerMovedEvent) {
    val removed = event.removedPrisonerNumber()
    val moved = event.movedPrisonerNumber()
    log.info("PRISONER MOVED EVENT: removed '$removed' moved with '$moved' - Not actioned")
  }
}
