package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerMergedEvent

@Component
class PrisonerMergedEventHandler : DomainEventHandler<PrisonerMergedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerMergedEvent) {
    val removed = event.removedPrisonerNumber()
    val replacement = event.replacementPrisonerNumber()
    log.info("PRISONER MERGED EVENT: Removed '$removed' replaced with '$replacement' - Not actioned")
  }
}
