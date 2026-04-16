package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerBookingMovedEventHandler
import java.time.LocalDateTime

class PrisonerBookingMovedEventHandlerTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()

  private val handler = PrisonerBookingMovedEventHandler(officialVisitRepository, prisonerVisitedRepository)

  @Test
  fun `should update visits and prisoner visited for a booking move event`() {
    val bookingStartDateTime = LocalDateTime.now()

    val bookingMoveEvent = PrisonerBookingMovedEvent(
      BookingMovedInformation("ABC222", "ABC111", "1", bookingStartDateTime),
    )

    whenever(officialVisitRepository.countOVByPrisonerNumberAndBookingId("ABC222", 1L, bookingStartDateTime)).thenReturn(2)

    handler.handle(bookingMoveEvent)

    verify(officialVisitRepository).bookingMove("ABC222", "ABC111", 1L, bookingStartDateTime)
    verify(prisonerVisitedRepository).replacePrisonerNumberForBooking("ABC222", "ABC111", 1L, bookingStartDateTime)
  }

  @Test
  fun `should not try to update visits when the count for prisoner number and booking ID is zero`() {
    val bookingStartDateTime = LocalDateTime.now()

    val bookingMoveEvent = PrisonerBookingMovedEvent(
      BookingMovedInformation("ABC222", "ABC111", "1", bookingStartDateTime),
    )

    whenever(officialVisitRepository.countOVByPrisonerNumberAndBookingId("ABC222", 1L, bookingStartDateTime)).thenReturn(0)

    handler.handle(bookingMoveEvent)

    verify(officialVisitRepository).countOVByPrisonerNumberAndBookingId("ABC222", 1L, bookingStartDateTime)

    verifyNoMoreInteractions(officialVisitRepository)
    verifyNoInteractions(prisonerVisitedRepository)
  }
}
