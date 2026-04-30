package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User

@Service
class OutboundEventsService(
  private val publisher: OutboundEventsPublisher,
  private val featureSwitches: FeatureSwitches,
  private val telemetryService: TelemetryService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  // Overloaded method for calls from the UI including a User object via the OfficialVisitsFacade
  fun send(
    outboundEvent: OutboundEvent,
    prisonCode: String,
    identifier: Long,
    secondIdentifier: Long? = 0,
    noms: String = "",
    contactId: Long? = null,
    source: Source = Source.DPS,
    user: User,
  ) = send(
    outboundEvent,
    prisonCode,
    identifier,
    secondIdentifier,
    noms,
    contactId,
    source,
    user.username,
  )

  // Standard method - for direct calls via the SyncFacade with no User object
  fun send(
    outboundEvent: OutboundEvent,
    prisonCode: String,
    identifier: Long,
    secondIdentifier: Long? = 0,
    noms: String = "",
    contactId: Long? = null,
    source: Source = Source.DPS,
    username: String,
  ) {
    if (featureSwitches.isEnabled(outboundEvent)) {
      log.info("Sending event $outboundEvent source $source identifier $identifier secondIdentifier ${secondIdentifier ?: "N/A"} noms $noms contactId ${contactId ?: "N/A"} ")

      when (outboundEvent) {
        OutboundEvent.VISIT_CREATED,
        OutboundEvent.VISIT_UPDATED,
        OutboundEvent.VISIT_CANCELLED,
        OutboundEvent.VISIT_DELETED,
        -> {
          sendSafely(
            outboundEvent,
            VisitInfo(identifier, source, username, prisonCode),
            PersonReference(noms),
          )
        }

        OutboundEvent.VISITOR_CREATED,
        OutboundEvent.VISITOR_UPDATED,
        OutboundEvent.VISITOR_DELETED,
        -> {
          sendSafely(
            outboundEvent,
            VisitorInfo(identifier, secondIdentifier ?: 0, source, username, prisonCode),
            contactId?.let { PersonReference(contactId = it) },
          )
        }

        OutboundEvent.PRISONER_UPDATED,
        -> {
          sendSafely(
            outboundEvent,
            PrisonerInfo(identifier, secondIdentifier ?: 0, source, username, prisonCode),
            PersonReference(noms),
          )
        }

        OutboundEvent.TIME_SLOT_CREATED,
        OutboundEvent.TIME_SLOT_UPDATED,
        OutboundEvent.TIME_SLOT_DELETED,
        -> {
          sendSafely(
            outboundEvent,
            TimeSlotInfo(identifier, source, username, prisonCode),
          )
        }

        OutboundEvent.VISIT_SLOT_CREATED,
        OutboundEvent.VISIT_SLOT_UPDATED,
        OutboundEvent.VISIT_SLOT_DELETED,
        -> {
          sendSafely(
            outboundEvent,
            VisitSlotInfo(identifier, source, username, prisonCode),
          )
        }
      }
    } else {
      log.warn("Outbound event type $outboundEvent feature is configured off.")
    }
  }

  private fun sendSafely(
    outboundEvent: OutboundEvent,
    additionalInformation: AdditionalInformation,
    personReference: PersonReference? = null,
  ) {
    try {
      val event = outboundEvent.event(additionalInformation, personReference)
      publisher.send(event)
      telemetryService.track(event)
    } catch (e: Exception) {
      log.error(
        "Unable to send event with type {}, info {}, person {}",
        outboundEvent,
        additionalInformation,
        personReference,
        e,
      )
    }
  }
}
