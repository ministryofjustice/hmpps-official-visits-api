package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!test && !local")
@Component
class InboundEventsListener(
  private val mapper: ObjectMapper,
  private val inboundEventsService: InboundEventsService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("official-visits", factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(rawMessage: String) {
    val message: Message = mapper.readValue(rawMessage)

    when (message.Type) {
      "Notification" -> {
        message.toDomainEventType()?.let { eventType ->
          runCatching {
            inboundEventsService.process(eventType.toInboundEvent(mapper, message.Message))
          }.onFailure {
            log.error("LISTENER: Error processing message ${message.MessageId}", it)
            throw it
          }
        } ?: log.info("LISTENER: Unrecognised event ${message.MessageAttributes.eventType.Value}")
      }
      else -> log.info("LISTENER: Ignoring message, actual message type '${message.Type}' is not a Notification.")
    }
  }
}

@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EventType(val Value: String, val Type: String)

data class MessageAttributes(val eventType: EventType)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class Message(
  val Type: String,
  val Message: String,
  val MessageId: String? = null,
  val MessageAttributes: MessageAttributes,
) {
  fun toDomainEventType() = DomainEventType.entries.singleOrNull { it.eventType == MessageAttributes.eventType.Value }
}
