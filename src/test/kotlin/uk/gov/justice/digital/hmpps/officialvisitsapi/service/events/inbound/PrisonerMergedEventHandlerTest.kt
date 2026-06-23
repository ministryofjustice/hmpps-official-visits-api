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
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerMergedEventHandler

class PrisonerMergedEventHandlerTest {
  private val removedPrisonerNumber = "A1234AA"
  private val retainedPrisonerNumber = "A1234BB"

  private val mergeEvent = PrisonerMergedEvent(
    additionalInformation = MergeInformation(
      nomsNumber = retainedPrisonerNumber,
      removedNomsNumber = removedPrisonerNumber,
      bookingId = "1",
    ),
  )
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val auditingService: AuditingService = mock()
  private val currentTermComponent: CurrentTermComponent = mock()

  private val handler = PrisonerMergedEventHandler(
    officialVisitRepository,
    prisonerVisitedRepository,
    auditingService,
    currentTermComponent,
  )

  @Test
  fun `should process a prisoner merge event and update visits`() {
    whenever(officialVisitRepository.findAllByPrisonerNumber(removedPrisonerNumber)).thenReturn(
      listOf(
        createAVisitEntity(1L),
        createAVisitEntity(2L),
      ),
    )

    handler.handle(mergeEvent)

    verify(prisonerVisitedRepository).replacePrisonerNumber(removedPrisonerNumber, retainedPrisonerNumber)
    verify(officialVisitRepository, times(2)).saveAndFlush(any())
    verify(auditingService, times(2)).recordAuditEvent(any())
    verify(currentTermComponent).processCurrentTermMarkers(
      prisonerNumber = retainedPrisonerNumber,
      source = "PRISONER MERGED EVENT",
      checkBookingId = mergeEvent.additionalInformation.bookingId.toLong(),
    )
  }

  @Test
  fun `should process a prisoner merge event but find no visits are affected`() {
    whenever(officialVisitRepository.findAllByPrisonerNumber(removedPrisonerNumber)).thenReturn(emptyList())

    handler.handle(mergeEvent)

    verify(officialVisitRepository).findAllByPrisonerNumber(removedPrisonerNumber)
    verifyNoMoreInteractions(officialVisitRepository)

    verifyNoInteractions(prisonerVisitedRepository, auditingService)

    verify(currentTermComponent).processCurrentTermMarkers(
      prisonerNumber = retainedPrisonerNumber,
      source = "PRISONER MERGED EVENT",
      checkBookingId = mergeEvent.additionalInformation.bookingId.toLong(),
    )
  }
}
