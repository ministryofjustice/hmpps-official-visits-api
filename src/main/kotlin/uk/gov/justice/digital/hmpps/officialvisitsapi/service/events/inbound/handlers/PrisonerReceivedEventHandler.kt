package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitSetToPreviousTermEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerReceivedEvent
import java.time.LocalDate
import kotlin.String

@Component
class PrisonerReceivedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val auditingService: AuditingService,
  private val prisonerSearchClient: PrisonerSearchClient,
) : DomainEventHandler<PrisonerReceivedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerReceivedEvent) {
    val prisonerNumber = event.prisonerNumber()
    val prisonCode = event.prisonCode()
    val reason = event.reason()

    // Get new prisoner details to obtain the most recent bookingId - the one notified by this event
    val prisoner = prisonerSearchClient.getPrisoner(prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner not found $prisonerNumber")

    if (event.indicatesANewBooking()) {
      log.info("PRISONER RECEIVED EVENT: Processing event for [$prisonCode] [$prisonerNumber] [${prisoner.bookingId}, reason [$reason] as it indicates a new booking")
      processNewBooking(prisonerNumber, prisoner.bookingId!!.toLong())
    } else {
      log.info("PRISONER RECEIVED EVENT: Ignoring event for [$prisonCode] [$prisonerNumber] [${prisoner.bookingId}], reason [${event.reason()}] as this does not indicate a new booking")
    }
  }

  /**
   * This method finds all visits for a prisoner number where the currentTerm == true and the bookingId on the
   * visit is different from the current bookingId. This avoids changing any visits for the currently active booking.
   *
   * It then updates these visits to set the currentTerm = false to indicate that they are now related to a previous
   * term in prison. No sync events are required for this operation as it is only reacting to the creation of a
   * new booking, not to any changes in visits or visitors.
   */
  private fun processNewBooking(prisonerNumber: String, bookingId: Long) {
    val visitsToUpdate = officialVisitRepository.findAllCurrentTermVisitsForPrisoner(prisonerNumber, bookingId)

    log.info("PRISONER RECEIVED EVENT: Found [${visitsToUpdate.size} visits to update for [$prisonerNumber]")

    visitsToUpdate.forEach { visit ->
      // Check whether this visit is in the future and SCHEDULED - if so log message.
      if (visit.visitStatusCode == VisitStatusType.SCHEDULED && visit.visitDate > LocalDate.now()) {
        log.info("PRISONER RECEIVED EVENT: OfficialVisitId [${visit.officialVisitId}] at [${visit.prisonCode}] for [$prisonerNumber] is still SCHEDULED [${visit.visitDate} but will be set to currentTerm = false")
      }

      // Update the currentTerm to false
      officialVisitRepository.saveAndFlush(visit.apply { currentTerm = false })

      // Create an audit event for the visit to record that it is no longer related to the current term in prison
      auditingService.recordAuditEvent(
        auditVisitSetToPreviousTermEvent {
          officialVisitId(visit.officialVisitId)
          summaryText("The visit is no longer related to the current term in prison")
          eventSource("NOMIS")
          user(UserService.getServiceAsUser())
          prisonCode(visit.prisonCode)
          prisonerNumber(prisonerNumber)
        },
      )
    }
  }
}
