package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditEventDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.CurrentTermComponent

/**
 * This test checks that the logic for amending the currentTerm marker on visits
 * is correct based on whether the visit.bookingId is the prisoner's current
 * booking or not.
 *
 * This component is used in most of the domain event handlers to recheck and reset
 * the current term marker after receiving an event which could alter it.
 */

class CurrentTermComponentTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val auditingService: AuditingService = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()

  val currentTermComponent = CurrentTermComponent(officialVisitRepository, prisonerSearchClient, auditingService)

  @BeforeEach
  fun setup() {
    whenever(prisonerSearchClient.getPrisoner(PENTONVILLE_PRISONER.number))
      .thenReturn(
        prisonerSearchPrisoner(
          prisonerNumber = PENTONVILLE_PRISONER.number, // this is 123456
          prisonCode = PENTONVILLE_PRISONER.prison, // this is PVI
          bookingId = PENTONVILLE_PRISONER.bookingId, // this is 1L
        ),
      )
  }

  @Test
  fun `should update current term marker for visit ID 1 to true, visit ID 3 to false, and leave visit ID 2 unchanged as true`() {
    // Stub two visits with booking ID 1L, with visit IDs 1L and 2L, current term true and false
    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(prisonerNumber = PENTONVILLE_PRISONER.number, bookingId = 1L))
      .thenReturn(
        listOf(
          createAVisitEntity(1L).apply {
            offenderBookId = 1L
            currentTerm = false
          },
          createAVisitEntity(2L).apply {
            offenderBookId = 1L
            currentTerm = true
          },
        ),
      )

    // Stub one visit with bookingId 2L and current term true
    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdNot(prisonerNumber = PENTONVILLE_PRISONER.number, bookingId = 1L))
      .thenReturn(
        listOf(
          createAVisitEntity(3L).apply {
            offenderBookId = 2L
            currentTerm = true
          },
        ),
      )

    currentTermComponent.processCurrentTermMarkers(PENTONVILLE_PRISONER.number, "PRISONER MERGED")

    verify(prisonerSearchClient).getPrisoner(PENTONVILLE_PRISONER.number)

    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookId(
      prisonerNumber = PENTONVILLE_PRISONER.number,
      bookingId = 1L,
    )

    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookIdNot(
      prisonerNumber = PENTONVILLE_PRISONER.number,
      bookingId = 1L,
    )

    val visitCaptor = argumentCaptor<OfficialVisitEntity>()

    verify(officialVisitRepository, times(2)).saveAndFlush(visitCaptor.capture())

    with(visitCaptor.firstValue) {
      assertThat(officialVisitId).isEqualTo(1L)
      assertThat(offenderBookId).isEqualTo(1L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(prisonCode).isEqualTo(PENTONVILLE)
      assertThat(currentTerm).isEqualTo(true)
    }

    with(visitCaptor.secondValue) {
      assertThat(officialVisitId).isEqualTo(3L)
      assertThat(offenderBookId).isEqualTo(2L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(prisonCode).isEqualTo(PENTONVILLE)
      assertThat(currentTerm).isEqualTo(false)
    }

    val auditEventCaptor = argumentCaptor<AuditEventDto>()

    verify(auditingService, times(2)).recordAuditEvent(auditEventCaptor.capture())

    with(auditEventCaptor.firstValue) {
      assertThat(officialVisitId).isEqualTo(1L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(prisonCode).isEqualTo(PENTONVILLE)
      assertThat(eventSource).isEqualTo("NOMIS")
      assertThat(summaryText).isEqualTo("Current term changed")
      assertThat(detailText).isEqualTo("current_term|false|true")
    }

    with(auditEventCaptor.secondValue) {
      assertThat(officialVisitId).isEqualTo(3L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(prisonCode).isEqualTo(PENTONVILLE)
      assertThat(eventSource).isEqualTo("NOMIS")
      assertThat(summaryText).isEqualTo("Current term changed")
      assertThat(detailText).isEqualTo("current_term|true|false")
    }
  }

  @Test
  fun `should not update current term markers for visits which already have the correct values`() {
    // Stub two visits with booking ID 1L, visit IDs 1L and 2L, current term both true - already correct
    whenever(
      officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(
        prisonerNumber = PENTONVILLE_PRISONER.number,
        bookingId = 1L,
      ),
    )
      .thenReturn(
        listOf(
          createAVisitEntity(1L).apply {
            offenderBookId = 1L
            currentTerm = true
          },
          createAVisitEntity(2L).apply {
            offenderBookId = 1L
            currentTerm = true
          },
        ),
      )

    // Stub one visit with booking ID 2L, visit ID 3L, and current term false - already correct
    whenever(
      officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdNot(
        prisonerNumber = PENTONVILLE_PRISONER.number,
        bookingId = 1L,
      ),
    )
      .thenReturn(
        listOf(
          createAVisitEntity(3L).apply {
            offenderBookId = 2L
            currentTerm = false
          },
        ),
      )

    currentTermComponent.processCurrentTermMarkers(PENTONVILLE_PRISONER.number, "BOOKING MOVED EVENT")

    verify(prisonerSearchClient).getPrisoner(PENTONVILLE_PRISONER.number)

    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookId(
      prisonerNumber = PENTONVILLE_PRISONER.number,
      bookingId = 1L,
    )

    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookIdNot(
      prisonerNumber = PENTONVILLE_PRISONER.number,
      bookingId = 1L,
    )

    // Verify no save and no audit recorded
    verifyNoMoreInteractions(officialVisitRepository)
    verifyNoInteractions(auditingService)
  }

  @Test
  fun `should update current term marker for visit ID's 1,2,3 and 4 to true and none to false`() {
    // Stub four visits with booking ID 1L, with visit IDs 1L, 2L, 3L, 4L and 2L, all with incorrect current term markers
    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(prisonerNumber = PENTONVILLE_PRISONER.number, bookingId = 1L))
      .thenReturn(
        listOf(
          createAVisitEntity(1L).apply {
            offenderBookId = 1L
            currentTerm = false
          },
          createAVisitEntity(2L).apply {
            offenderBookId = 1L
            currentTerm = false
          },
          createAVisitEntity(3L).apply {
            offenderBookId = 1L
            currentTerm = false
          },
          createAVisitEntity(4L).apply {
            offenderBookId = 1L
            currentTerm = false
          },
        ),
      )

    // No visits with booking IDs that is not 1L
    whenever(officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdNot(prisonerNumber = PENTONVILLE_PRISONER.number, bookingId = 1L))
      .thenReturn(emptyList())

    currentTermComponent.processCurrentTermMarkers(PENTONVILLE_PRISONER.number, "BOOKING DELETED EVENT")

    verify(prisonerSearchClient).getPrisoner(PENTONVILLE_PRISONER.number)

    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookId(
      prisonerNumber = PENTONVILLE_PRISONER.number,
      bookingId = 1L,
    )

    verify(officialVisitRepository).findAllByPrisonerNumberAndOffenderBookIdNot(
      prisonerNumber = PENTONVILLE_PRISONER.number,
      bookingId = 1L,
    )

    val visitCaptor = argumentCaptor<OfficialVisitEntity>()

    verify(officialVisitRepository, times(4)).saveAndFlush(visitCaptor.capture())

    with(visitCaptor.firstValue) {
      assertThat(officialVisitId).isEqualTo(1L)
      assertThat(offenderBookId).isEqualTo(1L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(currentTerm).isEqualTo(true)
    }

    with(visitCaptor.secondValue) {
      assertThat(officialVisitId).isEqualTo(2L)
      assertThat(offenderBookId).isEqualTo(1L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(currentTerm).isEqualTo(true)
    }

    with(visitCaptor.thirdValue) {
      assertThat(officialVisitId).isEqualTo(3L)
      assertThat(offenderBookId).isEqualTo(1L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(currentTerm).isEqualTo(true)
    }

    with(visitCaptor.lastValue) {
      assertThat(officialVisitId).isEqualTo(4L)
      assertThat(offenderBookId).isEqualTo(1L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(currentTerm).isEqualTo(true)
    }

    val auditEventCaptor = argumentCaptor<AuditEventDto>()

    verify(auditingService, times(4)).recordAuditEvent(auditEventCaptor.capture())

    with(auditEventCaptor.firstValue) {
      assertThat(officialVisitId).isEqualTo(1L)
      assertThat(summaryText).isEqualTo("Current term changed")
      assertThat(detailText).isEqualTo("current_term|false|true")
    }

    with(auditEventCaptor.secondValue) {
      assertThat(officialVisitId).isEqualTo(2L)
      assertThat(summaryText).isEqualTo("Current term changed")
      assertThat(detailText).isEqualTo("current_term|false|true")
    }

    with(auditEventCaptor.thirdValue) {
      assertThat(officialVisitId).isEqualTo(3L)
      assertThat(summaryText).isEqualTo("Current term changed")
      assertThat(detailText).isEqualTo("current_term|false|true")
    }

    with(auditEventCaptor.lastValue) {
      assertThat(officialVisitId).isEqualTo(4L)
      assertThat(summaryText).isEqualTo("Current term changed")
      assertThat(detailText).isEqualTo("current_term|false|true")
    }
  }

  @Test
  fun `should throw a runtime exception if prisoner search reports a different booking ID than the checkBookingId`() {
    // Provides a checkBookingId that is different from the prisoner search stub reports (1L)
    val exception = assertThrows<RuntimeException> {
      currentTermComponent.processCurrentTermMarkers(PENTONVILLE_PRISONER.number, "PRISONER MERGED EVENT", 2L)
    }

    assertThat(exception.message).isEqualTo(
      "Event PRISONER MERGED EVENT - Prisoner search ${PENTONVILLE_PRISONER.number} booking 1 is different from event booking 2",
    )

    verify(prisonerSearchClient).getPrisoner(PENTONVILLE_PRISONER.number)

    verifyNoInteractions(officialVisitRepository)
    verifyNoInteractions(auditingService)
  }
}
