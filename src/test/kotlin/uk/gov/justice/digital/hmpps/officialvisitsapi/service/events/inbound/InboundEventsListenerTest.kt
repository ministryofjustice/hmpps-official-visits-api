package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isInstanceOf
import java.util.UUID
import org.mockito.kotlin.check as mockitoCheck

class InboundEventsListenerTest {
  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
  private val inboundEventsService: InboundEventsService = mock()
  private val eventListener = InboundEventsListener(mapper, inboundEventsService)

  @Test
  fun `should delegate prisoner released domain event to service`() {
    val event = PrisonerReleasedEvent(
      ReleaseInformation(nomsNumber = "A1234AA", reason = "RELEASED", prisonId = "MDI"),
    )
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verify(inboundEventsService).process(
      mockitoCheck {
        it isInstanceOf PrisonerReleasedEvent::class.java
        (it as PrisonerReleasedEvent).additionalInformation.nomsNumber isEqualTo "A1234AA"
        it.additionalInformation.reason isEqualTo "RELEASED"
        it.additionalInformation.prisonId isEqualTo "MDI"
      },
    )
  }

  @Test
  fun `should delegate prisoner received domain event to service`() {
    val event = PrisonerReceivedEvent(
      ReceivedInformation(nomsNumber = "A1234AA", reason = "RECEIVED", prisonId = "MDI"),
    )
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verify(inboundEventsService).process(
      mockitoCheck {
        it isInstanceOf PrisonerReceivedEvent::class.java
        (it as PrisonerReceivedEvent).additionalInformation.nomsNumber isEqualTo "A1234AA"
        it.additionalInformation.reason isEqualTo "RECEIVED"
        it.additionalInformation.prisonId isEqualTo "MDI"
      },
    )
  }

  @Test
  fun `should delegate prisoner merged domain event to service`() {
    val event = PrisonerMergedEvent(
      MergeInformation(nomsNumber = "A1111AA", removedNomsNumber = "B1111BB"),
    )
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verify(inboundEventsService).process(
      mockitoCheck {
        it isInstanceOf PrisonerMergedEvent::class.java
        (it as PrisonerMergedEvent).additionalInformation.nomsNumber isEqualTo "A1111AA"
        it.additionalInformation.removedNomsNumber isEqualTo "B1111BB"
      },
    )
  }

  private fun message(event: DomainEvent<*>) = Message(
    "Notification",
    mapper.writeValueAsString(event),
    UUID.randomUUID().toString(),
    MessageAttributes(EventType(Type = "String", Value = event.eventType)),
  )
}
