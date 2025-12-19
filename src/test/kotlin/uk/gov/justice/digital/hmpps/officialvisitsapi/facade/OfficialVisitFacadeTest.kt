package uk.gov.justice.digital.hmpps.officialvisitsapi.facade

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitCreateService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEventsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class OfficialVisitFacadeTest {
  private val officialVisitCreateService: OfficialVisitCreateService = mock()
  private val officialVisitService: OfficialVisitsRetrievalService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val facade = OfficialVisitFacade(officialVisitCreateService, officialVisitService, outboundEventsService)
  private val user = PENTONVILLE_PRISON_USER

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

    whenever(officialVisitCreateService.create("MDI", request, user)).thenReturn(
      CreateOfficialVisitResponse(officialVisitId = 1L),
    )

    facade.createOfficialVisit("MDI", request, PENTONVILLE_PRISON_USER)

    verify(officialVisitCreateService).create("MDI", request, PENTONVILLE_PRISON_USER)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.VISIT_CREATED,
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
    facade.getOfficialVisitByPrisonCodeAndId("MIC", 1L)
    verify(officialVisitService).getOfficialVisitByPrisonCodeAndId("MIC", 1L)
  }
}
