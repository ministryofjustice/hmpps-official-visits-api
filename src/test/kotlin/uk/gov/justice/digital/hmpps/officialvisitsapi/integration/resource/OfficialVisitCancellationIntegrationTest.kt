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
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.PersonReference
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.PrisonerInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitorInfo
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

  private val nextMondayAt9 = createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor))

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
    stubEvents.reset()

    // Stub a known contact
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = CONTACT_MOORLAND_PRISONER.contactId,
          prisonerContactId = CONTACT_MOORLAND_PRISONER.prisonerContactId,
        ),
      ),
    )
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(prisonCode = MOORLAND, locations = listOf(location))
    locationsInsidePrisonApi().stubGetLocationById(location)
    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER)
    personalRelationshipsApi().stubReferenceGroup()
  }

  @Test
  fun `should cancel an official scheduled visit`() {
    auditedEventRepository.findAll() hasSize 0

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
      personReference = PersonReference(
        nomsNumber = cancelledVisit.prisonerVisited.prisonerNumber,
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
      personReference = PersonReference(
        contactId = 123,
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
      personReference = PersonReference(
        nomsNumber = cancelledVisit.prisonerVisited.prisonerNumber,
      ),
    )

    with(auditedEventRepository.findAll().single { it.summaryText == "Official visit cancelled" }) {
      officialVisitId isEqualTo cancelledVisit.officialVisitId
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      detailText isEqualTo "Visit cancelled by user ${MOORLAND_PRISON_USER.name}"
      userName isEqualTo MOORLAND_PRISON_USER.username
      userFullName isEqualTo MOORLAND_PRISON_USER.name
      eventSource isEqualTo Source.DPS.name
      eventDateTime isCloseTo now()
    }
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
