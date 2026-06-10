package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditEventType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitChangeEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerBookingMovedEvent

@Component
class PrisonerBookingMovedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val auditingService: AuditingService,
  private val currentTermComponent: CurrentTermComponent,
) : DomainEventHandler<PrisonerBookingMovedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerBookingMovedEvent) {
    val movedToNomsNumber = event.movedToNomsNumber()
    val movedFromNomsNumber = event.movedFromNomsNumber()
    val bookingId = event.bookingId().toLong()
    val startDateTime = event.bookingStartDateTime()

    log.info("Handling booking move from [$movedFromNomsNumber] to [$movedToNomsNumber] for booking [$bookingId]")

    // Find all visits for the prisoner and bookingId after the start date/time (these will move to the new prisoner number)
    val affectedVisits = officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual(
      movedFromNomsNumber,
      bookingId,
      startDateTime,
    )

    affectedVisits.takeIf { it.isNotEmpty() }?.let {
      // Update the prisoner number on affected visits
      officialVisitRepository.bookingMove(movedFromNomsNumber, movedToNomsNumber, bookingId, startDateTime)

      // Update the prisoner number on affected prisoner visited rows
      prisonerVisitedRepository.replacePrisonerNumberForBooking(movedFromNomsNumber, movedToNomsNumber, bookingId, startDateTime)

      // Record an audit event for affected visits to record the change of prisoner number
      affectedVisits.forEach { visit ->
        auditingService.recordAuditEvent(
          auditVisitChangeEvent {
            officialVisitId(visit.officialVisitId)
            summaryText(AuditEventType.PRISONER_BOOKING_MOVED)
            eventSource("NOMIS")
            user(UserService.getClientAsUser("NOMIS"))
            prisonCode(visit.prisonCode)
            prisonerNumber(movedToNomsNumber)
            changes {
              change("prisoner_number", movedFromNomsNumber, movedToNomsNumber)
            }
          },
        )
      }
    }

    // Check and reset current term markers for both prisoner numbers, which may or may not be affected
    currentTermComponent.processCurrentTermMarkers(movedFromNomsNumber, "BOOKING MOVED EVENT")
    currentTermComponent.processCurrentTermMarkers(movedToNomsNumber, "BOOKING MOVED EVENT")
  }
}
