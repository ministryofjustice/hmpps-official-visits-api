package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerBookingDeletedEventHandler
import java.time.LocalDateTime

class PrisonerBookingDeletedEventHandlerTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val auditingService: AuditingService = mock()

  private val handler = PrisonerBookingDeletedEventHandler(officialVisitRepository, auditingService)

  @Test
  fun `should update visits and set current term for a booking deleted event`() {
    val bookingStartDateTime = LocalDateTime.now()

    val bookingDeletedEvent = PrisonerBookingDeletedEvent(
      BookingDeletedInformation("1"),
      PersonReference(listOf(PersonIdentifier(Identifier.NOMS, "ABC222"))),
    )

    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual("ABC222", 1L, bookingStartDateTime)).thenReturn(
      listOf(createAVisitEntity(1L), createAVisitEntity(2L)),
    )

    handler.handle(bookingDeletedEvent)
  }

  @Test
  fun `should not try to update visits when the count for prisoner number and booking ID is zero`() {
    val bookingStartDateTime = LocalDateTime.now()

    val bookingDeletedEvent = PrisonerBookingDeletedEvent(
      BookingDeletedInformation("1"),
      PersonReference(listOf(PersonIdentifier(Identifier.NOMS, "ABC222"))),
    )

    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual("ABC222", 1L, bookingStartDateTime)).thenReturn(emptyList())

    handler.handle(bookingDeletedEvent)
  }
}
