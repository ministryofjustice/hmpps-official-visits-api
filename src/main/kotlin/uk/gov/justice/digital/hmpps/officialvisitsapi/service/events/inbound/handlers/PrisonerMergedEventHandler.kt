package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
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

    val prisoner = prisonerSearchClient.getPrisoner(newPrisonerNumber)
      ?: throw EntityNotFoundException("Prisoner not found $newPrisonerNumber")

    log.info("Prisoner merge from $removedPrisonerNumber to $newPrisonerNumber (on bookingId ${prisoner.bookingId?.toLong()})")

    val affectedVisits = officialVisitRepository.findAllByPrisonerNumber(removedPrisonerNumber)

    affectedVisits.takeIf { it.isNotEmpty() }?.let {
      officialVisitRepository.mergePrisonerNumber(removedPrisonerNumber, newPrisonerNumber)
      prisonerVisitedRepository.replacePrisonerNumber(removedPrisonerNumber, newPrisonerNumber)

      affectedVisits.forEach { visit ->
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

      // Check and correct the current term markers for this prisoner's bookings
      processCurrentTermMarkers(newPrisonerNumber, prisoner.bookingId!!.toLong())
    }
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
