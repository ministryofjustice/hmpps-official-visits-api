package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository

@Service
class PrisonerMergeService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  fun mergePrisoner(removedPrisonerNumber: String, newPrisonerNumber: String) {
    // Get new prisoner details
    val prisoner = prisonerSearchClient.getPrisoner(newPrisonerNumber)
    log.info("Booking Id for prisoner $newPrisonerNumber is  $prisoner?.bookingId?.toLong()")

    // get count of visits by old /removed prisoner number
    officialVisitRepository.countOVByPrisonerNumber(removedPrisonerNumber).takeIf { it > 0 }?.let { numberOfOVAffected ->
      officialVisitRepository.mergePrisonerNumber(removedPrisonerNumber, newPrisonerNumber, prisoner?.bookingId?.toLong())
      prisonerVisitedRepository.mergePrisonerNumber(removedPrisonerNumber, newPrisonerNumber)
    }
  }
}
