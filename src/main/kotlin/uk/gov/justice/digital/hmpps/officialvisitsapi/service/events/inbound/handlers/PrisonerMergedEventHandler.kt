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
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerMergedEvent

@Component
class PrisonerMergedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val auditingService: AuditingService,
  private val currentTermComponent: CurrentTermComponent,
) : DomainEventHandler<PrisonerMergedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerMergedEvent) {
    val removedPrisonerNumber = event.removedPrisonerNumber()
    val newPrisonerNumber = event.replacementPrisonerNumber()
    val bookingId = event.bookingId()

    log.info("Prisoner merge from $removedPrisonerNumber to $newPrisonerNumber (current bookingId ${bookingId.toLong()})")

    val affectedVisits = officialVisitRepository.findAllByPrisonerNumber(removedPrisonerNumber)

    affectedVisits.takeIf { it.isNotEmpty() }?.let {
      // This update is across all visits if any are present
      prisonerVisitedRepository.replacePrisonerNumber(removedPrisonerNumber, newPrisonerNumber)

      affectedVisits.forEach { visit ->
        officialVisitRepository.saveAndFlush(visit.apply { prisonerNumber = newPrisonerNumber })

        auditingService.recordAuditEvent(
          auditVisitChangeEvent {
            officialVisitId(visit.officialVisitId)
            summaryText(AuditEventType.PRISONER_MERGED)
            eventSource("NOMIS")
            user(UserService.getClientAsUser("NOMIS"))
            prisonCode(visit.prisonCode)
            prisonerNumber(newPrisonerNumber)
            changes {
              change("prisoner_number", removedPrisonerNumber, newPrisonerNumber)
            }
          },
        )
      }
    }

    // Check and update the current term markers on visits for the new prisoner only (the old prisoner number is removed)
    // This event provides a booking ID to check against the prisoner search bookingId - will throw an exception if different.
    currentTermComponent.processCurrentTermMarkers(newPrisonerNumber, "PRISONER MERGED EVENT", bookingId.toLong())
  }
}
