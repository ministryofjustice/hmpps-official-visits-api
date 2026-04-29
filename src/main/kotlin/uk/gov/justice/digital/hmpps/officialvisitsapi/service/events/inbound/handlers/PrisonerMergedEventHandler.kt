package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitChangeEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitCurrentTermEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerMergedEvent

@Component
class PrisonerMergedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val auditingService: AuditingService,
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
      prisonerVisitedRepository.replacePrisonerNumber(removedPrisonerNumber, newPrisonerNumber)

      affectedVisits.forEach { visit ->
        officialVisitRepository.saveAndFlush(visit.apply { prisonerNumber = newPrisonerNumber })

        auditingService.recordAuditEvent(
          auditVisitChangeEvent {
            officialVisitId(visit.officialVisitId)
            summaryText("Official visit updated by prisoner merge")
            eventSource("NOMIS")
            user(UserService.getClientAsUser("NOMIS"))
            prisonCode(visit.prisonCode)
            prisonerNumber(newPrisonerNumber)
            changes {
              change("Prisoner number", removedPrisonerNumber, newPrisonerNumber)
            }
          },
        )
      }
    }

    // Check and correct the current term markers for this prisoner's bookings - needs to be outside the empty check
    processCurrentTermMarkers(newPrisonerNumber, bookingId.toLong())
  }

  private fun processCurrentTermMarkers(prisonerNumber: String, bookingId: Long) {
    officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(prisonerNumber, bookingId).forEach { visit ->
      if (!visit.currentTerm) {
        officialVisitRepository.saveAndFlush(visit.apply { currentTerm = true })
        auditCurrentTermChange(visit)
      }
    }

    officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdNot(prisonerNumber, bookingId).forEach { visit ->
      if (visit.currentTerm) {
        officialVisitRepository.saveAndFlush(visit.apply { currentTerm = false })
        auditCurrentTermChange(visit)
      }
    }
  }

  private fun auditCurrentTermChange(visit: OfficialVisitEntity) = auditingService.recordAuditEvent(
    auditVisitCurrentTermEvent {
      officialVisitId(visit.officialVisitId)
      summaryText("The visit current term marker has been updated by a merge")
      eventSource("NOMIS")
      user(UserService.getServiceAsUser())
      prisonCode(visit.prisonCode)
      prisonerNumber(visit.prisonerNumber)
    },
  )
}
