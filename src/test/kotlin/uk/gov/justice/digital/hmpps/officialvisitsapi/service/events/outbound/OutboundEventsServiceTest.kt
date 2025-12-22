package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound

import org.assertj.core.api.Assertions.within
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.StandardTelemetryEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.TelemetryService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OutboundEventsServiceTest {
  private val publisher: OutboundEventsPublisher = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val telemetryService: TelemetryService = mock()
  private val outboundEventsService = OutboundEventsService(publisher, featureSwitches, telemetryService)
  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()
  private val telemetryCaptor = argumentCaptor<StandardTelemetryEvent>()
  private val aUser = MOORLAND_PRISON_USER

  @Test
  fun `official visit created event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.VISIT_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.VISIT_CREATED, MOORLAND, 1L, null, noms = "A1111AA", user = aUser)
    verify(
      expectedEventType = "official-visits-api.visit.created",
      expectedAdditionalInformation = VisitInfo(officialVisitId = 1L, source = Source.DPS, aUser.username, MOORLAND),
      expectedPersonReference = PersonReference(nomsNumber = "A1111AA"),
      expectedDescription = "An official visit has been created",
    )
  }

  @Test
  fun `official visit updated event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.VISIT_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.VISIT_UPDATED, MOORLAND, 1L, null, "A1111AA", user = aUser)
    verify(
      expectedEventType = "official-visits-api.visit.updated",
      expectedAdditionalInformation = VisitInfo(officialVisitId = 1L, source = Source.DPS, aUser.username, MOORLAND),
      expectedPersonReference = PersonReference(nomsNumber = "A1111AA"),
      expectedDescription = "An official visit has been updated",
    )
  }

  @Test
  fun `official visit cancelled event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.VISIT_CANCELLED) } doReturn true }
    outboundEventsService.send(OutboundEvent.VISIT_CANCELLED, MOORLAND, 1L, null, noms = "A1111AA", user = aUser)
    verify(
      expectedEventType = "official-visits-api.visit.cancelled",
      expectedAdditionalInformation = VisitInfo(officialVisitId = 1L, source = Source.DPS, aUser.username, MOORLAND),
      expectedPersonReference = PersonReference(nomsNumber = "A1111AA"),
      expectedDescription = "An official visit has been cancelled",
    )
  }

  @Test
  fun `visitor added to an official visit event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.VISITOR_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.VISITOR_CREATED, MOORLAND, 1L, 1L, contactId = 1L, user = aUser)
    verify(
      expectedEventType = "official-visits-api.visitor.created",
      expectedAdditionalInformation = VisitorInfo(officialVisitId = 1L, officialVisitorId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = PersonReference(contactId = 1L),
      expectedDescription = "A visitor has been added to an official visit",
    )
  }

  @Test
  fun `visitor updated on an official visit event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.VISITOR_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.VISITOR_UPDATED, MOORLAND, 1L, 1L, contactId = 1L, user = aUser)
    verify(
      expectedEventType = "official-visits-api.visitor.updated",
      expectedAdditionalInformation = VisitorInfo(officialVisitId = 1L, officialVisitorId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = PersonReference(contactId = 1L),
      expectedDescription = "A visitor on an official visit has been updated",
    )
  }

  @Test
  fun `visitor removed from an official visit event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.VISITOR_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.VISITOR_DELETED, MOORLAND, 1L, 1L, contactId = 1L, user = aUser)
    verify(
      expectedEventType = "official-visits-api.visitor.deleted",
      expectedAdditionalInformation = VisitorInfo(officialVisitId = 1L, officialVisitorId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = PersonReference(contactId = 1L),
      expectedDescription = "A visitor has been removed from an official visit",
    )
  }

  @Test
  fun `prisoner updated on an official visit event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_UPDATED, MOORLAND, 1L, 1L, noms = "A1111AA", user = aUser)
    verify(
      expectedEventType = "official-visits-api.prisoner.updated",
      expectedAdditionalInformation = PrisonerInfo(officialVisitId = 1L, prisonerVisitedId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = PersonReference(nomsNumber = "A1111AA"),
      expectedDescription = "A prisoner on an official visit has been updated",
    )
  }

  @Test
  fun `time slot created event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.TIME_SLOT_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.TIME_SLOT_CREATED, MOORLAND, 1L, null, noms = "", user = aUser)
    verify(
      expectedEventType = "official-visits-api.time-slot.created",
      expectedAdditionalInformation = TimeSlotInfo(timeSlotId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = null,
      expectedDescription = "An official visit time slot has been created",
    )
  }

  @Test
  fun `time slot updated event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.TIME_SLOT_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.TIME_SLOT_UPDATED, MOORLAND, 1L, null, noms = "", user = aUser)
    verify(
      expectedEventType = "official-visits-api.time-slot.updated",
      expectedAdditionalInformation = TimeSlotInfo(timeSlotId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = null,
      expectedDescription = "An official visit time slot has been updated",
    )
  }

  @Test
  fun `time slot deleted event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.TIME_SLOT_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.TIME_SLOT_DELETED, MOORLAND, 1L, null, noms = "", user = aUser)
    verify(
      expectedEventType = "official-visits-api.time-slot.deleted",
      expectedAdditionalInformation = TimeSlotInfo(timeSlotId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = null,
      expectedDescription = "An official visit time slot has been deleted",
    )
  }

  @Test
  fun `visit slot created event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.VISIT_SLOT_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.VISIT_SLOT_CREATED, MOORLAND, 1L, null, noms = "", user = aUser)
    verify(
      expectedEventType = "official-visits-api.visit-slot.created",
      expectedAdditionalInformation = VisitSlotInfo(visitSlotId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = null,
      expectedDescription = "An official visit slot has been created",
    )
  }

  @Test
  fun `visit slot amended event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.VISIT_SLOT_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.VISIT_SLOT_UPDATED, MOORLAND, 1L, null, noms = "", user = aUser)
    verify(
      expectedEventType = "official-visits-api.visit-slot.updated",
      expectedAdditionalInformation = VisitSlotInfo(visitSlotId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = null,
      expectedDescription = "An official visit slot has been updated",
    )
  }

  @Test
  fun `visit slot deleted event is published`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.VISIT_SLOT_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.VISIT_SLOT_DELETED, MOORLAND, 1L, null, noms = "", user = aUser)
    verify(
      expectedEventType = "official-visits-api.visit-slot.deleted",
      expectedAdditionalInformation = VisitSlotInfo(visitSlotId = 1L, source = Source.DPS, username = aUser.username, prisonId = MOORLAND),
      expectedPersonReference = null,
      expectedDescription = "An official visit slot has been deleted",
    )
  }

  @Test
  fun `events are not published for any outbound event when not enabled`() {
    featureSwitches.stub { on { isEnabled(any<OutboundEvent>(), any()) } doReturn false }
    OutboundEvent.entries.forEach { outboundEventsService.send(it, MOORLAND, 1L, 1L, user = aUser) }
    verifyNoInteractions(publisher)
  }

  @ParameterizedTest
  @EnumSource(OutboundEvent::class)
  fun `should trap exception sending event`(event: OutboundEvent) {
    featureSwitches.stub { on { isEnabled(event) } doReturn true }
    whenever(publisher.send(any())).thenThrow(RuntimeException("Boom!"))

    // Exceptions are logged and swallowed by the publisher
    outboundEventsService.send(event, MOORLAND, 1L, null, noms = "A1111AA", user = aUser)

    verify(publisher).send(any())
  }

  private fun verify(
    expectedEventType: String,
    expectedAdditionalInformation: AdditionalInformation,
    expectedPersonReference: PersonReference? = null,
    expectedOccurredAt: LocalDateTime = LocalDateTime.now(),
    expectedDescription: String,
  ) {
    verify(publisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo(expectedEventType)
      assertThat(additionalInformation).isEqualTo(expectedAdditionalInformation)
      assertThat(personReference?.contactId()).isEqualTo(expectedPersonReference?.contactId())
      assertThat(personReference?.nomsNumber()).isEqualTo(expectedPersonReference?.nomsNumber())
      assertThat(occurredAt).isCloseTo(expectedOccurredAt, within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo(expectedDescription)
    }

    verify(telemetryService).track(telemetryCaptor.capture())

    with(telemetryCaptor.firstValue) {
      assertThat(eventType).isEqualTo(expectedEventType)
      assertThat(properties()["prisoner_number"]).isEqualTo(expectedPersonReference?.nomsNumber())
      assertThat(properties()["contact_id"]).isEqualTo(expectedPersonReference?.contactId())
      assertThat(properties()["version"]).isEqualTo("1")
      assertThat(properties()["description"]).isEqualTo(expectedDescription)
      assertThat(properties()["source"]).isEqualTo(expectedAdditionalInformation.source.toString())
      assertThat(properties()["username"]).isEqualTo(expectedAdditionalInformation.username)
      assertThat(properties()["occurred_at"]).isNotNull()
      if (expectedAdditionalInformation.prisonId != null) {
        assertThat(properties()["prison_id"]).isEqualTo(expectedAdditionalInformation.prisonId)
      } else {
        assertThat(properties()["prison_id"]).isEqualTo("unknown")
      }
    }

    verifyNoMoreInteractions(publisher)
  }
}
