package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerBookingMovedEventHandler
import java.time.LocalDateTime

class PrisonerBookingMovedEventHandlerTest {
  private val movedBookingEvent = PrisonerBookingMovedEvent(
    BookingMovedInformation("ABC222", "ABC111", 1L, LocalDateTime.now()),
  )
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()

  private val handler = PrisonerBookingMovedEventHandler(officialVisitRepository, prisonerVisitedRepository)

  @Test
  fun `should merge old prisoner booking with new prisoner`() {
    val starTime = LocalDateTime.now()
    val mergeBooking = PrisonerBookingMovedEvent(
      BookingMovedInformation("ABC222", "ABC111", 1L, starTime),
    )
    whenever(officialVisitRepository.countOVByPrisonerNumberAndBookingId("ABC222", 1L, starTime)).thenReturn(2)
    handler.handle(mergeBooking)
    verify(officialVisitRepository).bookingMove("ABC222", "ABC111", 1L, starTime)
    verify(prisonerVisitedRepository).mergePrisonerNumber("ABC222", "ABC111")
  }

  @Test
  fun `should not merge if there are no matching booking found `() {
    whenever(officialVisitRepository.countOVByPrisonerNumberAndBookingId("ABC222", 1L, LocalDateTime.now())).thenReturn(0)
    handler.handle(movedBookingEvent)
    verifyNoInteractions(prisonerVisitedRepository)
  }
}
