package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditEventType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitCurrentTermEvent

/**
 * This is a shared component that is used by most of the domain event handlers
 * to find and reset the currentTerm markers on visits for a prisoner
 * based on whether the bookingId on the visit is their active booking.
 */

@Component
class CurrentTermComponent(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val auditingService: AuditingService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun processCurrentTermMarkers(prisonerNumber: String, source: String, checkBookingId: Long? = null) {
    var currentTermCounter = 0
    var previousTermCounter = 0

    // Get the current bookingId for this prisoner
    val prisoner = prisonerSearchClient.getPrisoner(prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner not found $prisonerNumber")

    // Check for prisoner search with a different booking ID than the check booking ID (only merges and booking moves provide this)
    // If the check fails this transaction is rolled back and the event will be retried from DLQ later.
    checkBookingId?.let {
      if (checkBookingId != prisoner.bookingId!!.toLong()) {
        log.info("Event $source - Prisoner search $prisonerNumber current booking ${prisoner.bookingId} is different than event booking $checkBookingId")
        throw RuntimeException("Event $source - Prisoner search $prisonerNumber booking ${prisoner.bookingId} is different from event booking $checkBookingId")
      }
    }

    // Find visits that match the prisoner number and current bookingId, if currentTerm = false set it to true
    officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(prisonerNumber, prisoner.bookingId!!.toLong())
      .filterNot { it.currentTerm }
      .forEach { visit ->
        officialVisitRepository.saveAndFlush(visit.apply { currentTerm = true })
        auditCurrentTermChange(visit, true)
        currentTermCounter++
      }

    // Find visits that match the prisoner number but NOT the current bookingId, if currentTerm = true set it to false
    officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdNot(prisonerNumber, prisoner.bookingId.toLong())
      .filter { it.currentTerm }
      .forEach { visit ->
        officialVisitRepository.saveAndFlush(visit.apply { currentTerm = false })
        auditCurrentTermChange(visit, false)
        previousTermCounter++
      }

    log.info("$source : Prisoner [$prisonerNumber] [$currentTermCounter] set to currentTerm = true, [$previousTermCounter] set to currentTerm = false")
  }

  private fun auditCurrentTermChange(visit: OfficialVisitEntity, currentTermMarker: Boolean) = auditingService.recordAuditEvent(
    auditVisitCurrentTermEvent {
      officialVisitId(visit.officialVisitId)
      summaryText("The visit current term marker has been updated by a merge")
      summaryText(AuditEventType.CURRENT_TERM_CHANGED)
      eventSource("NOMIS")
      user(UserService.getServiceAsUser())
      prisonCode(visit.prisonCode)
      prisonerNumber(visit.prisonerNumber)
      currentTerm(visit.currentTerm)
    },
  )
}
