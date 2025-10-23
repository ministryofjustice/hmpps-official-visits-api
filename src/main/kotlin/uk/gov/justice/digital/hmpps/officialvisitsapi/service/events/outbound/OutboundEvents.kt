package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound

import uk.gov.justice.digital.hmpps.officialvisitsapi.service.StandardTelemetryEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * An enum class containing all events that can be raised from the service.
 * Each can tailor its own AdditionalInformation and PersonReference content.
 */
enum class OutboundEvent(val eventType: String) {
  VISIT_CREATED("official-visits-api.visit.created") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "An official visit has been created",
    )
  },
  VISIT_UPDATED("official-visits-api.visit.updated") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "An official visit has been updated",
    )
  },
  VISIT_CANCELLED("official-visits-api.visit.cancelled") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "An official visit has been cancelled",
    )
  },
  VISITOR_CREATED("official-visits-api.visitor.created") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "A visitor has been added to an official visit",
    )
  },
  VISITOR_UPDATED("official-visits-api.visitor.updated") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "A visitor on an official visit has been updated",
    )
  },
  VISITOR_DELETED("official-visits-api.visitor.deleted") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "A visitor has been removed from an official visit",
    )
  },
  PRISONER_UPDATED("official-visits-api.prisoner.updated") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "A prisoner on an official visit has been updated",
    )
  },
  TIME_SLOT_CREATED("official-visits-api.time-slot.created") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "An official visit time slot has been created",
    )
  },
  TIME_SLOT_UPDATED("official-visits-api.time-slot.updated") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "An official visit time slot has been updated",
    )
  },
  TIME_SLOT_DELETED("official-visits-api.time-slot.deleted") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "An official visit time slot has been deleted",
    )
  },
  VISIT_SLOT_CREATED("official-visits-api.visit-slot.created") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "An official visit slot has been created",
    )
  },
  VISIT_SLOT_UPDATED("official-visits-api.visit-slot.updated") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "An official visit slot has been updated",
    )
  },
  VISIT_SLOT_DELETED("official-visits-api.visit-slot.deleted") {
    override fun event(additionalInformation: AdditionalInformation, personReference: PersonReference?) = OutboundHMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      personReference = personReference,
      description = "An official visit slot has been deleted",
    )
  }, ;

  abstract fun event(
    additionalInformation: AdditionalInformation,
    personReference: PersonReference? = null,
  ): OutboundHMPPSDomainEvent
}

/**
 * Base class for the additional information within events.
 * This is inherited and expanded individually for each event type.
 */

open class AdditionalInformation(
  open val source: Source,
  open val username: String,
  open val prisonId: String?,
)

/**
 * The class representing outbound domain events
 */
data class OutboundHMPPSDomainEvent(
  override val eventType: String,
  val additionalInformation: AdditionalInformation,
  val personReference: PersonReference? = null,
  val version: String = "1",
  val description: String,
  val occurredAt: LocalDateTime = LocalDateTime.now(),
) : StandardTelemetryEvent(eventType) {
  override fun properties() = listOfNotNull(
    personReference?.nomsNumber()?.let { "prisoner_number" to it },
    personReference?.contactId()?.let { "contact_id" to it },
    "version" to version,
    "description" to description,
    "occurred_at" to occurredAt.format(DateTimeFormatter.ISO_DATE_TIME),
    "source" to additionalInformation.source.toString(),
    "username" to additionalInformation.username,
    "prison_id" to (additionalInformation.prisonId ?: "unknown"),
  ).toMap()
}

/**
 * These are classes which define the different event content for AdditionalInformation.
 * All inherit the base class AdditionalInformation and extend it to contain the required variation of values.
 * The additional information is mapped into JSON by the ObjectMapper as part of the event body.
 */

data class VisitInfo(
  val officialVisitId: Long,
  override val source: Source = Source.DPS,
  override val username: String,
  override val prisonId: String?,
) : AdditionalInformation(source, username, prisonId)

data class VisitorInfo(
  val officialVisitId: Long,
  val officialVisitorId: Long,
  override val source: Source = Source.DPS,
  override val username: String,
  override val prisonId: String?,
) : AdditionalInformation(source, username, prisonId)

data class PrisonerInfo(
  val officialVisitId: Long,
  val prisonerVisitedId: Long,
  override val source: Source = Source.DPS,
  override val username: String,
  override val prisonId: String?,
) : AdditionalInformation(source, username, prisonId)

data class TimeSlotInfo(
  val timeSlotId: Long,
  override val source: Source = Source.DPS,
  override val username: String,
  override val prisonId: String?,
) : AdditionalInformation(source, username, prisonId)

data class VisitSlotInfo(
  val visitSlotId: Long,
  override val source: Source = Source.DPS,
  override val username: String,
  override val prisonId: String?,
) : AdditionalInformation(source, username, prisonId)

/**
 * The event source.
 * When data is changed within the DPS Official Visits service by UI action or local process, events will have the source DPS.
 * When data is changed as a result of receiving a sync event events will have the source NOMIS.
 */
enum class Source { DPS, NOMIS }

/**
 * Many events will provide a reference to a person (or people) via an identifier.
 * In some cases this will be the prisoner e.g. NOMS = prisonerNumber, and in others it will be
 * a visitor, i.e. the contact ID (or person ID in NOMIS).
 */
enum class Identifier { NOMS, CONTACT_ID }
data class PersonIdentifier(val type: Identifier, val value: String)

/**
 * The PersonReference contains the list of identifiers related to the subject of the event, if a person
 * is referenced. The two types of people referenced will be prisoners or visitors (contacts).
 * Some events will not refer to a person at all and the person reference will be omitted in that case.
 */
class PersonReference(personIdentifiers: List<PersonIdentifier>) {
  constructor(nomsNumber: String, visitorId: Long) : this(
    listOf(
      PersonIdentifier(Identifier.NOMS, nomsNumber),
      PersonIdentifier(Identifier.CONTACT_ID, visitorId.toString()),
    ),
  )

  constructor(nomsNumber: String) : this(
    listOf(
      PersonIdentifier(Identifier.NOMS, nomsNumber),
    ),
  )

  constructor(contactId: Long) : this(
    listOf(
      PersonIdentifier(Identifier.CONTACT_ID, contactId.toString()),
    ),
  )

  @Suppress("MemberVisibilityCanBePrivate")
  val identifiers: List<PersonIdentifier> = personIdentifiers

  fun nomsNumber(): String? = identifiers.find { it.type == Identifier.NOMS }?.value
  fun contactId(): String? = identifiers.find { it.type == Identifier.CONTACT_ID }?.value

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PersonReference

    return identifiers == other.identifiers
  }

  override fun hashCode(): Int = identifiers.hashCode()

  override fun toString(): String = this.identifiers.toString()
}
