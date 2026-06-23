package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.CurrentTermComponent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerBookingMovedEventHandler
import java.time.LocalDateTime

class PrisonerBookingMovedEventHandlerTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val auditingService: AuditingService = mock()
  private val currentTermComponent: CurrentTermComponent = mock()

  private val handler = PrisonerBookingMovedEventHandler(officialVisitRepository, prisonerVisitedRepository, auditingService, currentTermComponent)

  @Test
  fun `should process a booking moved event and update visits`() {
    val bookingStartDateTime = LocalDateTime.now().minusDays(1)

    val bookingMoveEvent = PrisonerBookingMovedEvent(
      BookingMovedInformation("ABC222", "ABC111", "1", bookingStartDateTime),
    )

    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual("ABC222", 1L, bookingStartDateTime)).thenReturn(
      listOf(createAVisitEntity(1L), createAVisitEntity(2L)),
    )

    handler.handle(bookingMoveEvent)

    verify(officialVisitRepository).bookingMove("ABC222", "ABC111", 1L, bookingStartDateTime)
    verify(prisonerVisitedRepository).replacePrisonerNumberForBooking("ABC222", "ABC111", 1L, bookingStartDateTime)
    verify(auditingService, times(2)).recordAuditEvent(any())
    verify(currentTermComponent).processCurrentTermMarkers("ABC111", "BOOKING MOVED EVENT", bookingMoveEvent.bookingId().toLong())
    verify(currentTermComponent).processCurrentTermMarkers("ABC222", "BOOKING MOVED EVENT")
  }

  @Test
  fun `should not update visits when none are affected by the booking move event`() {
    val bookingStartDateTime = LocalDateTime.now()

    val bookingMoveEvent = PrisonerBookingMovedEvent(
      BookingMovedInformation("ABC222", "ABC111", "1", bookingStartDateTime),
    )

    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual("ABC222", 1L, bookingStartDateTime)).thenReturn(emptyList())

    handler.handle(bookingMoveEvent)

    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual("ABC222", 1L, bookingStartDateTime)

    verifyNoMoreInteractions(officialVisitRepository)
    verifyNoInteractions(prisonerVisitedRepository)
    verify(currentTermComponent).processCurrentTermMarkers("ABC111", "BOOKING MOVED EVENT", bookingMoveEvent.bookingId().toLong())
    verify(currentTermComponent).processCurrentTermMarkers("ABC222", "BOOKING MOVED EVENT")
  }
}
