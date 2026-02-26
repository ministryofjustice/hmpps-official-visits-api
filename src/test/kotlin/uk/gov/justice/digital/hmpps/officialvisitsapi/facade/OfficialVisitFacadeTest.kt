package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateCommentRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitVisitorUpdate
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitorUpdated
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCancellationService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCompletionService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitSearchService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitUpdateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class OfficialVisitFacadeTest {
  private val officialVisitCreateService: OfficialVisitCreateService = mock()
  private val officialVisitsRetrievalService: OfficialVisitsRetrievalService = mock()
  private val officialVisitSearchService: OfficialVisitSearchService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val officialVisitCompletionService: OfficialVisitCompletionService = mock()
  private val officialVisitCancellationService: OfficialVisitCancellationService = mock()
  private val officialVisitUpdateService: OfficialVisitUpdateService = mock()
  private val user = MOORLAND_PRISON_USER

  private val facade = OfficialVisitFacade(
    officialVisitCreateService,
    officialVisitsRetrievalService,
    officialVisitSearchService,
    officialVisitCompletionService,
    officialVisitCancellationService,
    officialVisitUpdateService,
    outboundEventsService,
  )

  @Test
  fun `should delegate to service on create and emit visit and visitor created events`() {
    val request = CreateOfficialVisitRequest(
      prisonVisitSlotId = 1L,
      prisonerNumber = "A1234AA",
      visitDate = LocalDate.now().plusDays(1),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationId = UUID.randomUUID(),
      visitTypeCode = VisitType.IN_PERSON,
      staffNotes = "staff",
      prisonerNotes = "prisoner",
      officialVisitors = listOf(
        OfficialVisitor(
          visitorTypeCode = VisitorType.CONTACT,
          contactId = 1L,
          prisonerContactId = 2L,
          officialVisitorId = 0L,
        ),
      ),
    )

    whenever(officialVisitCreateService.create(MOORLAND, request, user)).thenReturn(
      CreateOfficialVisitResponse(officialVisitId = 1L, officialVisitorIds = listOf(2L)),
    )

    facade.createOfficialVisit(MOORLAND, request, MOORLAND_PRISON_USER)

    verify(officialVisitCreateService).create(MOORLAND, request, MOORLAND_PRISON_USER)

    // An event for the visit created
    verify(outboundEventsService, atLeastOnce()).send(
      outboundEvent = OutboundEvent.VISIT_CREATED,
      prisonCode = MOORLAND,
      identifier = 1L, // visitId
      noms = "A1234AA",
      source = Source.DPS,
      user = user,
    )

    // An event for the visitor created
    verify(outboundEventsService, atLeastOnce()).send(
      outboundEvent = OutboundEvent.VISITOR_CREATED,
      prisonCode = MOORLAND,
      identifier = 1L, // visitId
      secondIdentifier = 2L, // visitorId
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should delegate to service to fetch official visits based on Id`() {
    facade.getOfficialVisitByPrisonCodeAndId(MOORLAND, 1L)

    verify(officialVisitsRetrievalService).getOfficialVisitByPrisonCodeAndId(MOORLAND, 1L)
  }

  @Test
  fun `should delegate to correct service on search`() {
    val request: OfficialVisitSummarySearchRequest = mock()

    facade.searchForOfficialVisitSummaries(MOORLAND, request, 0, 10)

    verify(officialVisitSearchService).searchForOfficialVisitSummaries(MOORLAND, request, 0, 10)
  }

  @Test
  fun `should fail create on caseload check if user is not in the correct caseload`() {
    val request: CreateOfficialVisitRequest = mock()

    assertThrows<CaseloadAccessException> {
      facade.createOfficialVisit(MOORLAND, request, PENTONVILLE_PRISON_USER)
    }
      .message isEqualTo "This visit cannot be created in a prison which is not the active caseload for the user"
  }

  @Test
  fun `should fail create when user is not a prison user`() {
    val request: CreateOfficialVisitRequest = mock()

    assertThrows<IllegalArgumentException> {
      facade.createOfficialVisit(MOORLAND, request, UserService.getServiceAsUser())
    }
      .message isEqualTo "Visits can only be created by a digital prison user"
  }

  @Test
  fun `should update visit type and slot details for an official scheduled visit`() {
    val request: OfficialVisitUpdateSlotRequest = mock()
    facade.updateVisitTypeAndSlot(1, MOORLAND, request, MOORLAND_PRISON_USER)
    Mockito.verify(officialVisitUpdateService).updateVisitTypeAndSlot(1, MOORLAND, request, MOORLAND_PRISON_USER)
    // should emit update event
    Mockito.verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISIT_UPDATED,
      prisonCode = MOORLAND,
      identifier = 1,
      user = user,
    )
  }

  @Test
  fun `should update prisoner and staff notes details for an official scheduled visit`() {
    val request: OfficialVisitUpdateCommentRequest = mock()

    facade.updateComments(1, MOORLAND, request, MOORLAND_PRISON_USER)
    Mockito.verify(officialVisitUpdateService).updateComments(1, MOORLAND, request, MOORLAND_PRISON_USER)
    // should emit update event
    Mockito.verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISIT_UPDATED,
      prisonCode = MOORLAND,
      identifier = 1,
      user = user,
    )
  }

  @Test
  fun `should update existing visitors details, add new visitor and remove existing visitor for an official scheduled visit`() {
    val request: OfficialVisitUpdateVisitorsRequest = mock()
    val response = OfficialVisitVisitorUpdate(
      officialVisitId = 1,
      prisonCode = MOORLAND,
      prisonerNumber = MOORLAND_PRISONER.number,
      visitorsAdded = listOf(
        OfficialVisitorUpdated(
          officialVisitorId = 1,
          contactId = 1,
        ),
      ),
      visitorsDeleted = listOf(
        OfficialVisitorUpdated(
          officialVisitorId = 2,
          contactId = 2,
        ),
      ),
      visitorsUpdated = listOf(
        OfficialVisitorUpdated(
          officialVisitorId = 3,
          contactId = 3,
        ),
      ),
    )
    whenever(officialVisitUpdateService.updateVisitors(1, MOORLAND, request, MOORLAND_PRISON_USER)).thenReturn(response)

    facade.updateVisitors(1, MOORLAND, request, MOORLAND_PRISON_USER)
    Mockito.verify(officialVisitUpdateService).updateVisitors(1, MOORLAND, request, MOORLAND_PRISON_USER)

    // should emit create,update and delete events
    Mockito.verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISITOR_UPDATED,
      prisonCode = MOORLAND,
      identifier = 1,
      secondIdentifier = 3,
      contactId = 3,
      user = user,
      source = Source.DPS,
    )
    Mockito.verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISITOR_DELETED,
      prisonCode = MOORLAND,
      identifier = 1,
      secondIdentifier = 2,
      contactId = 2,
      user = user,
      source = Source.DPS,
    )
    Mockito.verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISITOR_CREATED,
      prisonCode = MOORLAND,
      identifier = 1,
      secondIdentifier = 1,
      contactId = 1,
      user = user,
      source = Source.DPS,
    )
  }
}
