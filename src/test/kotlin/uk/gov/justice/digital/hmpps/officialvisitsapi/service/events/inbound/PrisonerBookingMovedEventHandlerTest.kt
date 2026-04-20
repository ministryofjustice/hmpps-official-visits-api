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
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerBookingMovedEventHandler
import java.time.LocalDateTime

class PrisonerBookingMovedEventHandlerTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val auditingService: AuditingService = mock()

  private val handler = PrisonerBookingMovedEventHandler(officialVisitRepository, prisonerVisitedRepository, auditingService)

  @Test
  fun `should update visits and prisoner visited for a booking move event`() {
    val bookingStartDateTime = LocalDateTime.now()

    val bookingMoveEvent = PrisonerBookingMovedEvent(
      BookingMovedInformation("ABC222", "ABC111", "1", bookingStartDateTime),
    )

    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual("ABC222", 1L, bookingStartDateTime)).thenReturn(
      listOf(createAVisitEntity(1L), createAVisitEntity(2L)),
    )

    handler.handle(bookingMoveEvent)

    verify(officialVisitRepository).bookingMove("ABC222", "ABC111", 1L, bookingStartDateTime)
    verify(prisonerVisitedRepository).replacePrisonerNumber("ABC222", "ABC111")
    verify(auditingService, times(2)).recordAuditEvent(any())
  }

  @Test
  fun `should not try to update visits when the count for prisoner number and booking ID is zero`() {
    val bookingStartDateTime = LocalDateTime.now()

    val bookingMoveEvent = PrisonerBookingMovedEvent(
      BookingMovedInformation("ABC222", "ABC111", "1", bookingStartDateTime),
    )

    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual("ABC222", 1L, bookingStartDateTime)).thenReturn(emptyList())

    handler.handle(bookingMoveEvent)

    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual("ABC222", 1L, bookingStartDateTime)

    verifyNoMoreInteractions(officialVisitRepository)
    verifyNoInteractions(prisonerVisitedRepository)
  }
}
