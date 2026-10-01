package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerReleasedEvent

/**
 * This handler is not currently doing anything useful with release events.
 * If staff choose to cancel visits in NOMIS the standard sync process will cancel it in DPS
 * If they choose not to cancel on release or transfer the visit remains in DPS too.
 * Automatic cancellation was considered, but not taken up.
 */
@Component
class PrisonerReleasedEventHandler : DomainEventHandler<PrisonerReleasedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerReleasedEvent) {
    val prisonerNumber = event.prisonerNumber()
    val prison = event.prisonId()
    when {
      event.isTemporary() -> log.info("TEMPORARY RELEASE EVENT: Ignoring temporary release (no action) - $prisonerNumber from $prison")
      event.isTransferred() -> log.info("TRANSFER EVENT: Ignoring transfer (handled by sync) - $prisonerNumber from $prison")
      event.isPermanent() -> log.info("PERMANENT RELEASE EVENT: Ignoring permanent release (handled by sync) - $prisonerNumber from $prison")
      else -> log.warn("RELEASE EVENT HANDLER: Ignoring unknown release event $event")
    }
  }
}
