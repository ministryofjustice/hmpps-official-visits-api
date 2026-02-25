package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.PrisonerInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitorInfo
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class OfficialVisitCancellationIntegrationTest : IntegrationTestBase() {

  private val location = moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))

  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = 123,
    prisonerContactId = 456,
    leadVisitor = true,
    assistedVisit = false,
    assistedNotes = "visitor notes",
    visitorEquipment = VisitorEquipment("Bringing secure laptop"),
  )

  private final val visitDateInTheFuture = today().next(DayOfWeek.MONDAY)

  private val nextMondayAt9 = CreateOfficialVisitRequest(
    prisonerNumber = MOORLAND_PRISONER.number,
    prisonVisitSlotId = 1,
    visitDate = visitDateInTheFuture,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = location.id,
    visitTypeCode = VisitType.IN_PERSON,
    staffNotes = "private notes",
    prisonerNotes = "public notes",
    searchTypeCode = SearchLevelType.PAT,
    officialVisitors = listOf(officialVisitor),
  )

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
    stubEvents.reset()

    personalRelationshipsApi().stubAllApprovedContacts(CONTACT_MOORLAND_PRISONER)
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(prisonCode = MOORLAND, locations = listOf(location))
    locationsInsidePrisonApi().stubGetLocationById(location)
  }

  @Test
  fun `should cancel an official scheduled visit`() {
    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER)
    personalRelationshipsApi().stubReferenceGroup()

    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)
      .let { response -> testAPIClient.getOfficialVisitBy(response.officialVisitId, MOORLAND_PRISON_USER) }

    with(scheduledVisit) {
      visitStatus isEqualTo VisitStatusType.SCHEDULED
      completionCode isEqualTo null
      searchTypeCode isEqualTo null
      updatedBy isEqualTo null
      updatedTime isEqualTo null
    }

    with(scheduledVisit.officialVisitors!!.single()) {
      attendanceCode isEqualTo null
      updatedBy isEqualTo null
      updatedTime isEqualTo null
    }

    webTestClient.cancel(
      officialVisitId = scheduledVisit.officialVisitId,
      request = OfficialVisitCancellationRequest(
        cancellationReason = VisitCompletionType.VISITOR_CANCELLED,
        cancellationNotes = "cancelled by integration test",
      ),
    )

    val cancelledVisit = testAPIClient.getOfficialVisitBy(scheduledVisit.officialVisitId, MOORLAND_PRISON_USER)

    with(cancelledVisit) {
      visitStatus isEqualTo VisitStatusType.CANCELLED
      completionCode isEqualTo VisitCompletionType.VISITOR_CANCELLED
      completionNotes isEqualTo "cancelled by integration test"
      searchTypeCode isEqualTo null
      updatedBy isEqualTo MOORLAND_PRISON_USER.username
      updatedTime isCloseTo now()
    }

    with(cancelledVisit.prisonerVisited!!) {
      attendanceCode isEqualTo AttendanceType.ABSENT.name
    }

    with(cancelledVisit.officialVisitors!!.single()) {
      attendanceCode isEqualTo AttendanceType.ABSENT
      updatedBy isEqualTo MOORLAND_PRISON_USER.username
      updatedTime isCloseTo now()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_UPDATED,
      additionalInfo = VisitInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitId = cancelledVisit.officialVisitId,
      ),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_UPDATED,
      additionalInfo = VisitorInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitorId = cancelledVisit.officialVisitors.single().officialVisitorId,
        officialVisitId = cancelledVisit.officialVisitId,
      ),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_UPDATED,
      additionalInfo = PrisonerInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        prisonerVisitedId = prisonerVisitedRepository.findByOfficialVisitId(cancelledVisit.officialVisitId)!!.prisonerVisitedId,
        officialVisitId = cancelledVisit.officialVisitId,
      ),
    )
  }

  private fun WebTestClient.cancel(officialVisitId: Long, request: OfficialVisitCancellationRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .post()
    .uri("/official-visit/prison/${prisonUser.activeCaseLoadId}/id/$officialVisitId/cancel")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
}
