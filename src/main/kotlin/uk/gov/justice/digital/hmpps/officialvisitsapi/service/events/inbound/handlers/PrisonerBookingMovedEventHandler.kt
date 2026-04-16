package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerBookingMovedEvent

@Component
class PrisonerBookingMovedEventHandler(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
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

    officialVisitRepository.countOVByPrisonerNumberAndBookingId(movedFromNomsNumber, bookingId, startDateTime).takeIf { it > 0 }?.let {
      officialVisitRepository.bookingMove(movedFromNomsNumber, movedToNomsNumber, bookingId, startDateTime)
      prisonerVisitedRepository.replacePrisonerNumberForBooking(movedFromNomsNumber, movedToNomsNumber, bookingId, startDateTime)
    }
  }
}
