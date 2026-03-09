package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerMergedEvent

@Component
class PrisonerMergedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
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

    log.info("Booking Id for prisoner $newPrisonerNumber is  $prisoner.bookingId?.toLong()")

    // get count of visits by old /removed prisoner number
    officialVisitRepository.countOVByPrisonerNumber(removedPrisonerNumber).takeIf { it > 0 }?.let {
      officialVisitRepository.mergePrisonerNumber(removedPrisonerNumber, newPrisonerNumber, prisoner.bookingId?.toLong())
      prisonerVisitedRepository.replacePrisonerNumber(removedPrisonerNumber, newPrisonerNumber)
    }
    log.info("PRISONER MERGED EVENT: Removed '$removedPrisonerNumber' replaced with '$newPrisonerNumber' ")
  }
}
