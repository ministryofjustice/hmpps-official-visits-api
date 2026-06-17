package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.CurrentTermComponent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerBookingDeletedEventHandler

class PrisonerBookingDeletedEventHandlerTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val auditingService: AuditingService = mock()
  private val currentTermComponent: CurrentTermComponent = mock()

  private val handler = PrisonerBookingDeletedEventHandler(officialVisitRepository, auditingService, currentTermComponent)

  @Test
  fun `should record an audit event and process current term markers for a booking deleted event`() {
    val bookingDeletedEvent = PrisonerBookingDeletedEvent(
      BookingDeletedInformation("1"),
      PersonReference(listOf(PersonIdentifier(Identifier.NOMS, "ABC222"))),
    )

    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId("ABC222", 1L)).thenReturn(
      listOf(createAVisitEntity(1L), createAVisitEntity(2L)),
    )

    handler.handle(bookingDeletedEvent)

    verify(auditingService, times(2)).recordAuditEvent(any())
    verify(currentTermComponent).processCurrentTermMarkers("ABC222", "BOOKING DELETED EVENT")
  }

  @Test
  fun `should not record an audit event but will still process current term markers`() {
    val bookingDeletedEvent = PrisonerBookingDeletedEvent(
      BookingDeletedInformation("1"),
      PersonReference(listOf(PersonIdentifier(Identifier.NOMS, "ABC222"))),
    )

    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId("ABC222", 1L)).thenReturn(emptyList())

    handler.handle(bookingDeletedEvent)

    verifyNoInteractions(auditingService)
    verify(currentTermComponent).processCurrentTermMarkers("ABC222", "BOOKING DELETED EVENT")
  }
}
