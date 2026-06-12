package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerBookingDeletedEvent

@Component
class PrisonerBookingDeletedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val auditingService: AuditingService,
) : DomainEventHandler<PrisonerBookingDeletedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerBookingDeletedEvent) {
    val bookingId = event.bookingId().toLong()
    val prisonerNumber = event.prisonerNumber()
    log.warn("Received booking deleted event for booking [$bookingId] with prisoner number [$prisonerNumber]")
    return
  }
}
