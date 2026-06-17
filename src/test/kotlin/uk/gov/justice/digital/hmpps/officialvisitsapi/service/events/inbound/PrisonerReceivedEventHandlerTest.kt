package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.CurrentTermComponent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerReceivedEventHandler

class PrisonerReceivedEventHandlerTest {
  private val currentTermComponent: CurrentTermComponent = mock()

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

  private val handler = PrisonerReceivedEventHandler(currentTermComponent)

  @Test
  fun `should adjust the currentTerm markers on visits for a NEW_ADMISSION event`() {
    handler.handle(newBookingEvent)
    verify(currentTermComponent).processCurrentTermMarkers(PENTONVILLE_PRISONER.number, "PRISONER RECEIVED EVENT")
  }

  @Test
  fun `should adjust the currentTerm markers on visits for a READMISSION_SWITCH_BOOKING event`() {
    handler.handle(newSwitchBookingEvent)
    verify(currentTermComponent).processCurrentTermMarkers(PENTONVILLE_PRISONER.number, "PRISONER RECEIVED EVENT")
  }

  @Test
  fun `should not attempt to adjust the currentTerm markers when a new booking is not created`() {
    handler.handle(sameBookingEvent)
    verifyNoInteractions(currentTermComponent)
  }
}
