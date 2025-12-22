package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitSearchService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
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
  private val user = MOORLAND_PRISON_USER

  private val facade = OfficialVisitFacade(officialVisitCreateService, officialVisitsRetrievalService, officialVisitSearchService, outboundEventsService)

  @Test
  fun `should delegate to service on create and emit a visit created event`() {
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
        ),
      ),
    )

    whenever(officialVisitCreateService.create(MOORLAND, request, user)).thenReturn(
      CreateOfficialVisitResponse(officialVisitId = 1L),
    )

    facade.createOfficialVisit(MOORLAND, request, MOORLAND_PRISON_USER)

    verify(officialVisitCreateService).create(MOORLAND, request, MOORLAND_PRISON_USER)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISIT_CREATED,
      MOORLAND,
      identifier = 1L,
      secondIdentifier = 0L,
      noms = "A1234AA",
      contactId = null,
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
}
