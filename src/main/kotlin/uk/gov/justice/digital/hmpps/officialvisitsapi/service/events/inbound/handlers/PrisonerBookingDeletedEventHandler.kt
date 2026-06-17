package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitChangeEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerBookingDeletedEvent

@Component
class PrisonerBookingDeletedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val auditingService: AuditingService,
  private val currentTermComponent: CurrentTermComponent,
) : DomainEventHandler<PrisonerBookingDeletedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerBookingDeletedEvent) {
    val bookingId = event.bookingId().toLong()
    val prisonerNumber = event.prisonerNumber()!!

    log.info("Received booking deleted event for bookingId [$bookingId] prisoner number [$prisonerNumber]")

    // Find all visits for this prisoner number and bookingId (they are affected by this event)
    val affectedVisits = officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(
      prisonerNumber,
      bookingId,
    )

    // Record an audit event for affected visits
    affectedVisits.takeIf { it.isNotEmpty() }?.let {
      affectedVisits.forEach { visit ->
        auditingService.recordAuditEvent(
          auditVisitChangeEvent {
            officialVisitId(visit.officialVisitId)
            summaryText("Official visit updated due to its booking being deleted")
            eventSource("NOMIS")
            user(UserService.getClientAsUser("NOMIS"))
            prisonCode(visit.prisonCode)
            prisonerNumber(prisonerNumber)
            changes {
              change("Booking deleted", event.bookingId(), "")
            }
          },
        )
      }
    }

    // Adjust current term markers on the visits for this prisoner
    currentTermComponent.processCurrentTermMarkers(prisonerNumber, "BOOKING DELETED EVENT")
  }
}
