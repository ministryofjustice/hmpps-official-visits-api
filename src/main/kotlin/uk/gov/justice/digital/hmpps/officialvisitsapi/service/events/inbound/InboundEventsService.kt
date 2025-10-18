package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerMergedEventHandler
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerReceivedEventHandler
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerReleasedEventHandler

@Service
class InboundEventsService(
  private val prisonerReleasedEventHandler: PrisonerReleasedEventHandler,
  private val prisonerMergedEventHandler: PrisonerMergedEventHandler,
  private val prisonerReceivedEventHandler: PrisonerReceivedEventHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun process(event: DomainEvent<*>) {
    when (event) {
      is PrisonerMergedEvent -> prisonerMergedEventHandler.handle(event)
      is PrisonerReleasedEvent -> prisonerReleasedEventHandler.handle(event)
      is PrisonerReceivedEvent -> prisonerReceivedEventHandler.handle(event)
      else -> log.warn("Unsupported domain event ${event.javaClass.name}")
    }
  }
}
