package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerReleasedEventHandler

/**
 * These tests just log output of the received messages.
 * Built to be extended if and when any action is taken automatically on release.
 */

class PrisonerReleasedEventHandlerTest {
  val permanentReleaseEvent = PrisonerReleasedEvent(
    additionalInformation = ReleaseInformation(
      nomsNumber = PENTONVILLE_PRISONER.number,
      reason = "RELEASED",
      prisonId = PENTONVILLE,
    ),
  )

  private val temporaryReleaseEvent = PrisonerReleasedEvent(
    additionalInformation = ReleaseInformation(
      nomsNumber = PENTONVILLE_PRISONER.number,
      reason = "SENT_TO_COURT",
      prisonId = PENTONVILLE,
    ),
  )

  private val transferEvent = PrisonerReleasedEvent(
    additionalInformation = ReleaseInformation(
      nomsNumber = PENTONVILLE_PRISONER.number,
      reason = "TRANSFERRED",
      prisonId = PENTONVILLE,
    ),
  )

  private val handler = PrisonerReleasedEventHandler()

  @Test
  fun `should handle and ignore a permanent release event`() {
    handler.handle(permanentReleaseEvent)
  }

  @Test
  fun `should handle and ignore a transfer event`() {
    handler.handle(transferEvent)
  }

  @Test
  fun `should handle and ignore a temporary release event`() {
    handler.handle(temporaryReleaseEvent)
  }
}
