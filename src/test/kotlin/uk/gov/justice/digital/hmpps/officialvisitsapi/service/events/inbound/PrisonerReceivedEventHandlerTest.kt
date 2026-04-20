package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerReceivedEventHandler

class PrisonerReceivedEventHandlerTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val auditingService: AuditingService = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()

  // Indicates received on a new booking ID
  private val newBookingEvent = PrisonerReceivedEvent(
    additionalInformation = ReceivedInformation(
      nomsNumber = PENTONVILLE_PRISONER.number,
      reason = "NEW_ADMISSION",
      prisonId = PENTONVILLE,
    ),
  )

  // Indicates received on a new booking ID
  private val newSwitchBookingEvent = PrisonerReceivedEvent(
    additionalInformation = ReceivedInformation(
      nomsNumber = PENTONVILLE_PRISONER.number,
      reason = "READMISSION_SWITCH_BOOKING",
      prisonId = PENTONVILLE,
    ),
  )

  // Indicates received on the existing booking ID
  private val sameBookingEvent = PrisonerReceivedEvent(
    additionalInformation = ReceivedInformation(
      nomsNumber = PENTONVILLE_PRISONER.number,
      reason = "RETURN_FROM_COURT",
      prisonId = PENTONVILLE,
    ),
  )

  private val handler = PrisonerReceivedEventHandler(officialVisitRepository, auditingService, prisonerSearchClient)

  @BeforeEach
  fun setup() {
    whenever(
      prisonerSearchClient.getPrisoner(PENTONVILLE_PRISONER.number),
    ).thenReturn(
      prisonerSearchPrisoner(
        prisonerNumber = PENTONVILLE_PRISONER.number,
        prisonCode = PENTONVILLE_PRISONER.prison,
        bookingId = PENTONVILLE_PRISONER.bookingId, // BookingId = 1L
      ),
    )

    whenever(
      officialVisitRepository.findAllByPrisonerNumberAndOffenderBookId(
        prisonerNumber = PENTONVILLE_PRISONER.number,
        bookingId = 1L,
      ),
    ).thenReturn(
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

    whenever(
      officialVisitRepository.findAllByPrisonerNumberAndOffenderBookIdNot(
        prisonerNumber = PENTONVILLE_PRISONER.number,
        bookingId = 1L,
      ),
    ).thenReturn(
      listOf(
        createAVisitEntity(3L).apply {
          offenderBookId = 2L
          currentTerm = true
        },
      ),
    )
  }

  /**
   * This test should update bookingIds 1 and 3, but leave 2 unchanged, as its current term
   * marker is already correct for the circumstances.
   */
  @Test
  fun `should manage the currentTerm markers on visits for a NEW_ADMISSION event`() {
    handler.handle(newBookingEvent)

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
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(prisonCode).isEqualTo(PENTONVILLE)
      assertThat(currentTerm).isEqualTo(true)
    }

    with(visitCaptor.secondValue) {
      assertThat(officialVisitId).isEqualTo(3L)
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
      assertThat(summaryText).isEqualTo("The visit current term marker has been updated")
    }

    with(auditEventCaptor.secondValue) {
      assertThat(officialVisitId).isEqualTo(3L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(prisonCode).isEqualTo(PENTONVILLE)
      assertThat(summaryText).isEqualTo("The visit current term marker has been updated")
    }
  }

  /**
   * This test should update bookingIds 1 and 3, but leave 2 unchanged, as its current term
   * marker is already correct for the circumstances.
   */
  @Test
  fun `should manage the currentTerm markers on visits for a READMISSION_SWITCH_BOOKING event`() {
    handler.handle(newSwitchBookingEvent)

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
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(prisonCode).isEqualTo(PENTONVILLE)
      assertThat(currentTerm).isEqualTo(true)
    }

    with(visitCaptor.secondValue) {
      assertThat(officialVisitId).isEqualTo(3L)
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
      assertThat(summaryText).isEqualTo("The visit current term marker has been updated")
    }

    with(auditEventCaptor.secondValue) {
      assertThat(officialVisitId).isEqualTo(3L)
      assertThat(prisonerNumber).isEqualTo(PENTONVILLE_PRISONER.number)
      assertThat(prisonCode).isEqualTo(PENTONVILLE)
      assertThat(summaryText).isEqualTo("The visit current term marker has been updated")
    }
  }

  @Test
  fun `should not attempt to set the currentTerm marker to false when a new booking is not created`() {
    handler.handle(sameBookingEvent)

    verify(prisonerSearchClient).getPrisoner(PENTONVILLE_PRISONER.number)
    verifyNoInteractions(officialVisitRepository)
    verifyNoInteractions(auditingService)
  }
}
