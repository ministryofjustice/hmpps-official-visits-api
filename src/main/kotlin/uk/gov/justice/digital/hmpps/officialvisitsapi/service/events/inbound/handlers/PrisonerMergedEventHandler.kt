package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitChangeEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerMergedEvent

@Component
class PrisonerMergedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val auditingService: AuditingService,
) : DomainEventHandler<PrisonerMergedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerMergedEvent) {
    val removedPrisonerNumber = event.removedPrisonerNumber()
    val newPrisonerNumber = event.replacementPrisonerNumber()

    // Get new prisoner details
    val prisoner = prisonerSearchClient.getPrisoner(newPrisonerNumber)
      ?: throw EntityNotFoundException("Prisoner not found $newPrisonerNumber")

    log.info("Booking Id for prisoner $newPrisonerNumber is $prisoner.bookingId?.toLong()")

    // get all affected visits before bulk update so each update is auditable by visit id
    val affectedVisits = officialVisitRepository.findAllByPrisonerNumber(removedPrisonerNumber)

    affectedVisits.takeIf { it.isNotEmpty() }?.let {
      officialVisitRepository.mergePrisonerNumber(removedPrisonerNumber, newPrisonerNumber, prisoner.bookingId?.toLong())
      prisonerVisitedRepository.replacePrisonerNumber(removedPrisonerNumber, newPrisonerNumber)

      affectedVisits.forEach { visit ->
        auditingService.recordAuditEvent(
          auditVisitChangeEvent {
            officialVisitId(visit.officialVisitId)
            summaryText("Official visit updated due to prisoner merge")
            eventSource("NOMIS")
            user(UserService.getClientAsUser("NOMIS"))
            prisonCode(visit.prisonCode)
            prisonerNumber(newPrisonerNumber)
            changes {
              change("Prisoner number", removedPrisonerNumber, newPrisonerNumber)
              change("Offender book ID", visit.offenderBookId, prisoner.bookingId?.toLong())
            }
          },
        )
      }
    }
    log.info("PRISONER MERGED EVENT: Removed '$removedPrisonerNumber' replaced with '$newPrisonerNumber' ")
  }
}
