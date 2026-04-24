package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerMergedEventHandler

class PrisonerMergedEventHandlerTest {
  private val removedPrisonerNumber = "A1234AA"
  private val retainedPrisonerNumber = "A1234BB"
  private val mergeEvent = PrisonerMergedEvent(
    additionalInformation = MergeInformation(nomsNumber = retainedPrisonerNumber, removedPrisonerNumber),
  )
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val auditingService: AuditingService = mock()

  private val handler = PrisonerMergedEventHandler(officialVisitRepository, prisonerVisitedRepository, prisonerSearchClient, auditingService)

  @Test
  fun `should merge old prisoner with new prisoner`() {
    whenever(prisonerSearchClient.getPrisoner(retainedPrisonerNumber)) doReturn prisonerSearchPrisoner(
      prisonerNumber = retainedPrisonerNumber,
      prisonCode = MOORLAND_PRISONER.prison,
      bookingId = MOORLAND_PRISONER.bookingId,
    )

    whenever(officialVisitRepository.findAllByPrisonerNumber(removedPrisonerNumber)).thenReturn(
      listOf(
        createAVisitEntity(1L),
        createAVisitEntity(2L),
      ),
    )

    // No current term markers to update
    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(retainedPrisonerNumber, MOORLAND_PRISONER.bookingId)).thenReturn(emptyList())
    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdNot(retainedPrisonerNumber, MOORLAND_PRISONER.bookingId)).thenReturn(emptyList())

    handler.handle(mergeEvent)

    // Do the prisoner updates - once each
    verify(officialVisitRepository).mergePrisonerNumber(removedPrisonerNumber, retainedPrisonerNumber)
    verify(prisonerVisitedRepository).replacePrisonerNumber(removedPrisonerNumber, retainedPrisonerNumber)

    // Check the current term markers - once each
    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookId(retainedPrisonerNumber, MOORLAND_PRISONER.bookingId)
    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookIdNot(retainedPrisonerNumber, MOORLAND_PRISONER.bookingId)

    // Record updates on 2 visits
    verify(auditingService, times(2)).recordAuditEvent(any())
  }

  @Test
  fun `should merge and update current term marker on an existing visit`() {
    whenever(prisonerSearchClient.getPrisoner(retainedPrisonerNumber)) doReturn prisonerSearchPrisoner(
      prisonerNumber = retainedPrisonerNumber,
      prisonCode = MOORLAND_PRISONER.prison,
      bookingId = MOORLAND_PRISONER.bookingId,
    )

    // One visit to update
    whenever(officialVisitRepository.findAllByPrisonerNumber(removedPrisonerNumber)).thenReturn(
      listOf(createAVisitEntity(1L)),
    )

    // One current term marker to update to true
    whenever(
      officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(
        retainedPrisonerNumber,
        MOORLAND_PRISONER.bookingId,
      ),
    ).thenReturn(listOf(createAVisitEntity(2L).apply { currentTerm = false }))

    // One current term marker to update to false
    whenever(
      officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdNot(
        retainedPrisonerNumber,
        MOORLAND_PRISONER.bookingId,
      ),
    ).thenReturn(listOf(createAVisitEntity(3L).apply { currentTerm = true }))

    handler.handle(mergeEvent)

    // Called once each
    verify(officialVisitRepository).mergePrisonerNumber(removedPrisonerNumber, retainedPrisonerNumber)
    verify(prisonerVisitedRepository).replacePrisonerNumber(removedPrisonerNumber, retainedPrisonerNumber)

    // Called once each
    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookId(retainedPrisonerNumber, MOORLAND_PRISONER.bookingId)
    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookIdNot(retainedPrisonerNumber, MOORLAND_PRISONER.bookingId)

    // Called 3 times - once for the visit update, twice for current term updates
    verify(auditingService, times(3)).recordAuditEvent(any())
  }

  @Test
  fun `should not not call merge if no official visits exist for the removed prisoner number`() {
    whenever(prisonerSearchClient.getPrisoner(retainedPrisonerNumber)) doReturn prisonerSearchPrisoner(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonCode = MOORLAND_PRISONER.prison,
      bookingId = MOORLAND_PRISONER.bookingId,
    )

    whenever(officialVisitRepository.findAllByPrisonerNumber(removedPrisonerNumber)).thenReturn(emptyList())

    handler.handle(mergeEvent)

    verify(officialVisitRepository).findAllByPrisonerNumber(removedPrisonerNumber)
    verifyNoMoreInteractions(officialVisitRepository)
    verifyNoInteractions(prisonerVisitedRepository, auditingService)
  }

  @Test
  fun `should throw exception if invalid prisoner passed`() {
    whenever(prisonerSearchClient.getPrisoner(retainedPrisonerNumber)).thenThrow(EntityNotFoundException("Prisoner not found $retainedPrisonerNumber"))
    assertThrows<EntityNotFoundException> {
      handler.handle(mergeEvent)
    }
    verifyNoInteractions(prisonerVisitedRepository, officialVisitRepository, auditingService)
  }
}
