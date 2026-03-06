package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerMergedEventHandler

class PrisonerMergedEventHandlerTest {
  private val mergeEvent = PrisonerMergedEvent(MergeInformation("ABC222", "ABC111"))
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()

  private val handler = PrisonerMergedEventHandler(officialVisitRepository, prisonerVisitedRepository, prisonerSearchClient)

  @Test
  fun `should merge old prisoner with new prisoner`() {
    whenever(prisonerSearchClient.getPrisoner("ABC222")) doReturn prisonerSearchPrisoner(prisonerNumber = MOORLAND_PRISONER.number, prisonCode = MOORLAND_PRISONER.prison, bookingId = MOORLAND_PRISONER.bookingId)
    whenever(officialVisitRepository.countOVByPrisonerNumber("ABC111")).thenReturn(1)
    handler.handle(mergeEvent)
    verify(officialVisitRepository).mergePrisonerNumber("ABC111", "ABC222", 1L)
    verify(prisonerVisitedRepository).mergePrisonerNumber("ABC111", "ABC222")
  }

  @Test
  fun `should not merge old prisoner with new prisoner if no official visits exists for old prisoner`() {
    whenever(prisonerSearchClient.getPrisoner("ABC222")) doReturn prisonerSearchPrisoner(prisonerNumber = MOORLAND_PRISONER.number, prisonCode = MOORLAND_PRISONER.prison, bookingId = MOORLAND_PRISONER.bookingId)
    whenever(officialVisitRepository.countOVByPrisonerNumber("ABC111")).thenReturn(0)
    handler.handle(mergeEvent)
    verifyNoInteractions(prisonerVisitedRepository)
  }

  @Test
  fun `should throw exception if invalid prisoner passed`() {
    whenever(prisonerSearchClient.getPrisoner("ABC222")).thenThrow(EntityNotFoundException("Prisoner not found ABC222"))
    assertThrows<EntityNotFoundException> {
      handler.handle(mergeEvent)
    }
    verifyNoInteractions(prisonerVisitedRepository, officialVisitRepository)
  }
}
