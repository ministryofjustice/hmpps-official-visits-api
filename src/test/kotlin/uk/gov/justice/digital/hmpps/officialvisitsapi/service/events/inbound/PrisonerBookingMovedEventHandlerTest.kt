package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerBookingMovedEventHandler

class PrisonerBookingMovedEventHandlerTest {
  private val mergeBookingEvent = PrisonerBookingMovedEvent(BookingMovedInformation("ABC222", "ABC111", 1L))
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()

  private val handler = PrisonerBookingMovedEventHandler(officialVisitRepository, prisonerVisitedRepository)

  @Test
  fun `should merge old prisoner booking with new prisoner`() {
    whenever(officialVisitRepository.countOVByPrisonerNumberAndBookingId("ABC222", 1L)).thenReturn(1)
    handler.handle(mergeBookingEvent)
    verify(officialVisitRepository).mergePrisonersBooking("ABC222", "ABC111", 1L)
    verify(prisonerVisitedRepository).mergePrisonerNumber("ABC222", "ABC111")
  }

  @Test
  fun `should not merge if there are no matching booking found `() {
    whenever(officialVisitRepository.countOVByPrisonerNumberAndBookingId("ABC222", 1L)).thenReturn(0)
    handler.handle(mergeBookingEvent)
    verifyNoInteractions(prisonerVisitedRepository)
  }
}
