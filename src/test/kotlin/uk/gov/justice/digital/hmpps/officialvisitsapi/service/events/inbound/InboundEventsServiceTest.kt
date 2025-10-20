package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerMergedEventHandler
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerReceivedEventHandler
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerReleasedEventHandler

class InboundEventsServiceTest {
  private val prisonerReleasedEventHandler: PrisonerReleasedEventHandler = mock()
  private val prisonerMergedEventHandler: PrisonerMergedEventHandler = mock()
  private val prisonerReceivedEventHandler: PrisonerReceivedEventHandler = mock()

  private val service = InboundEventsService(
    prisonerReleasedEventHandler,
    prisonerMergedEventHandler,
    prisonerReceivedEventHandler,
  )

  @Test
  fun `should call prisoner released handler when for prisoner released event`() {
    val event1 = PrisonerReleasedEvent(ReleaseInformation("A1111AA", "RELEASED", BIRMINGHAM))
    service.process(event1)
    verify(prisonerReleasedEventHandler).handle(event1)

    val event2 = PrisonerReleasedEvent(ReleaseInformation("B1111BB", "RELEASED", BIRMINGHAM))
    service.process(event2)
    verify(prisonerReleasedEventHandler).handle(event2)
  }

  @Test
  fun `should call prisoner merged handler when for prisoner merged event`() {
    val event = PrisonerMergedEvent(MergeInformation(nomsNumber = "A1111AA", removedNomsNumber = "B1111BB"))
    service.process(event)
    verify(prisonerMergedEventHandler).handle(event)
  }

  @Test
  fun `should call prisoner received event handler when prisoner received event`() {
    val event = mock<PrisonerReceivedEvent>()
    service.process(event)
    verify(prisonerReceivedEventHandler).handle(event)
  }
}
