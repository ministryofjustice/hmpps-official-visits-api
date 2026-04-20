package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitCurrentTermEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerReceivedEvent
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
   * This method:
   *  - finds visits for the prisoner where the bookingId == the new booking, and sets currentTerm = true if it is false.
   *  - finds visits for the prisoner where the bookingId != the new booking, and sets currentTerm = false if it is true.
   *
   * No sync events are required for this operation as these changes have already been applied in NOMIS.
   */
  private fun processNewBooking(prisonerNumber: String, bookingId: Long) {
    var currentTermCounter = 0
    var previousTermCounter = 0

    officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(prisonerNumber, bookingId).forEach { visit ->
      if (!visit.currentTerm) {
        officialVisitRepository.saveAndFlush(visit.apply { currentTerm = true })
        auditCurrentTermChange(visit)
        currentTermCounter++
      }
    }

    officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdNot(prisonerNumber, bookingId).forEach { visit ->
      if (visit.currentTerm) {
        officialVisitRepository.saveAndFlush(visit.apply { currentTerm = false })
        auditCurrentTermChange(visit)
        previousTermCounter++
      }
    }

    log.info("PRISONER RECEIVED EVENT: [$currentTermCounter] visits set to currentTerm = true, [$previousTermCounter] set to currentTerm = false")
  }

  private fun auditCurrentTermChange(visit: OfficialVisitEntity) = auditingService.recordAuditEvent(
    auditVisitCurrentTermEvent {
      officialVisitId(visit.officialVisitId)
      summaryText("The visit current term marker has been updated")
      eventSource("NOMIS")
      user(UserService.getServiceAsUser())
      prisonCode(visit.prisonCode)
      prisonerNumber(visit.prisonerNumber)
    },
  )
}
