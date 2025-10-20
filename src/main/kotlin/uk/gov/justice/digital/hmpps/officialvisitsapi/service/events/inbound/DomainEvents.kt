package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toOffsetString
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Identifier
import java.time.LocalDateTime

abstract class DomainEvent<T : AdditionalInformation>(
  eventType: DomainEventType,
  val additionalInformation: T,
) {
  val eventType = eventType.eventType
  val description = eventType.description
  val version: String = "1"
  val occurredAt: String = LocalDateTime.now().toOffsetString()

  fun toEventType() = DomainEventType.valueOf(eventType)

  override fun toString() = this::class.simpleName + " - (eventType = $eventType, , occurredAt = $occurredAt, additionalInformation = $additionalInformation)"
}

interface AdditionalInformation

class PrisonerReceivedEvent(additionalInformation: ReceivedInformation) : DomainEvent<ReceivedInformation>(DomainEventType.PRISONER_RECEIVED, additionalInformation) {
  fun prisonerNumber() = additionalInformation.nomsNumber
  fun prisonCode() = additionalInformation.prisonId
}

data class ReceivedInformation(val nomsNumber: String, val reason: String, val prisonId: String) : AdditionalInformation

class PrisonerReleasedEvent(additionalInformation: ReleaseInformation) : DomainEvent<ReleaseInformation>(DomainEventType.PRISONER_RELEASED, additionalInformation) {

  @JsonIgnore
  fun prisonerNumber() = additionalInformation.nomsNumber

  @JsonIgnore
  fun prisonId() = additionalInformation.prisonId

  @JsonIgnore
  fun isTemporary() = listOf("TEMPORARY_ABSENCE_RELEASE", "SENT_TO_COURT").contains(additionalInformation.reason)

  @JsonIgnore
  fun isTransferred() = listOf("TRANSFERRED").contains(additionalInformation.reason)

  @JsonIgnore
  fun isPermanent() = listOf("RELEASED", "RELEASED_TO_HOSPITAL").contains(additionalInformation.reason)
}

data class ReleaseInformation(val nomsNumber: String, val reason: String, val prisonId: String) : AdditionalInformation

class PrisonerMergedEvent(additionalInformation: MergeInformation) : DomainEvent<MergeInformation>(DomainEventType.PRISONER_MERGED, additionalInformation) {
  fun replacementPrisonerNumber() = additionalInformation.nomsNumber
  fun removedPrisonerNumber() = additionalInformation.removedNomsNumber
}

data class MergeInformation(val nomsNumber: String, val removedNomsNumber: String) : AdditionalInformation

data class PersonReference(val identifiers: List<Identifier>)

data class Identifier(val type: String, val value: String)

enum class DomainEventType(val eventType: String, val description: String = "") {
  PRISONER_RECEIVED("prisoner-offender-search.prisoner.received") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) = mapper.readValue<PrisonerReceivedEvent>(message)
  },
  PRISONER_RELEASED("prisoner-offender-search.prisoner.released") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) = mapper.readValue<PrisonerReleasedEvent>(message)
  },
  PRISONER_MERGED("prison-offender-events.prisoner.merged") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) = mapper.readValue<PrisonerMergedEvent>(message)
  },
  ;

  abstract fun toInboundEvent(mapper: ObjectMapper, message: String): DomainEvent<*>
}
