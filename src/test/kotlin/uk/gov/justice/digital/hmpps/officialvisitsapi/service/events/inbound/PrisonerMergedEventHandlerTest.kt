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
  private val mergeEvent = PrisonerMergedEvent(MergeInformation("ABC222", "ABC111"))
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val auditingService: AuditingService = mock()

  private val handler = PrisonerMergedEventHandler(officialVisitRepository, prisonerVisitedRepository, prisonerSearchClient, auditingService)

  @Test
  fun `should merge old prisoner with new prisoner`() {
    whenever(prisonerSearchClient.getPrisoner("ABC222")) doReturn prisonerSearchPrisoner(prisonerNumber = MOORLAND_PRISONER.number, prisonCode = MOORLAND_PRISONER.prison, bookingId = MOORLAND_PRISONER.bookingId)
    whenever(officialVisitRepository.findAllByPrisonerNumber("ABC111")).thenReturn(listOf(createAVisitEntity(1L), createAVisitEntity(2L)))
    handler.handle(mergeEvent)
    verify(officialVisitRepository).mergePrisonerNumber("ABC111", "ABC222", 1L)
    verify(prisonerVisitedRepository).replacePrisonerNumber("ABC111", "ABC222")
    verify(auditingService, times(2)).recordAuditEvent(any())
  }

  @Test
  fun `should not merge old prisoner with new prisoner if no official visits exists for old prisoner`() {
    whenever(prisonerSearchClient.getPrisoner("ABC222")) doReturn prisonerSearchPrisoner(prisonerNumber = MOORLAND_PRISONER.number, prisonCode = MOORLAND_PRISONER.prison, bookingId = MOORLAND_PRISONER.bookingId)
    whenever(officialVisitRepository.findAllByPrisonerNumber("ABC111")).thenReturn(emptyList())
    handler.handle(mergeEvent)
    verifyNoInteractions(prisonerVisitedRepository, auditingService)
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
