package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitChangeEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerBookingMovedEvent

@Component
class PrisonerBookingMovedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val auditingService: AuditingService,
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
    val affectedVisits = officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual(
      movedFromNomsNumber,
      bookingId,
      startDateTime,
    )

    // update prisoner booking if exists
    affectedVisits.takeIf { it.isNotEmpty() }?.let {
      officialVisitRepository.bookingMove(movedFromNomsNumber, movedToNomsNumber, bookingId, startDateTime)
      prisonerVisitedRepository.replacePrisonerNumberForBooking(movedFromNomsNumber, movedToNomsNumber, bookingId, startDateTime)

      affectedVisits.forEach { visit ->
        auditingService.recordAuditEvent(
          auditVisitChangeEvent {
            officialVisitId(visit.officialVisitId)
            summaryText("Official visit updated due to prisoner booking move")
            eventSource("NOMIS")
            user(UserService.getClientAsUser("NOMIS"))
            prisonCode(visit.prisonCode)
            prisonerNumber(movedToNomsNumber)
            changes {
              change("Prisoner number", movedFromNomsNumber, movedToNomsNumber)
            }
          },
        )
      }
    }
  }
}
