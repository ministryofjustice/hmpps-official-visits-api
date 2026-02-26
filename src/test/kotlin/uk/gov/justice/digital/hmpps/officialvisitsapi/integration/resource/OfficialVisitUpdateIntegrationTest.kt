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
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.pagedModelPrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateCommentRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitorInfo
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class OfficialVisitUpdateIntegrationTest : IntegrationTestBase() {
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
    officialVisitors = listOf(
      officialVisitor,
      OfficialVisitor(
        visitorTypeCode = VisitorType.CONTACT,
        relationshipCode = "POM2",
        contactId = 130,
        prisonerContactId = 460,
        leadVisitor = true,
        assistedVisit = false,
        assistedNotes = "visitor notes",
        visitorEquipment = VisitorEquipment("Bringing secure laptop"),
      ),
    ),
  )

  private val updateSlot = OfficialVisitUpdateSlotRequest(
    prisonVisitSlotId = 1,
    visitDate = visitDateInTheFuture.plusMonths(20),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    dpsLocationId = location.id,
    visitTypeCode = VisitType.VIDEO,
  )

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
    stubEvents.reset()

    personalRelationshipsApi().stubAllApprovedContacts(CONTACT_MOORLAND_PRISONER)
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(
      prisonCode = MOORLAND,
      locations = listOf(location),
    )
    locationsInsidePrisonApi().stubGetLocationById(location)
  }

  @Test
  fun `should update visit type and slot details for an official scheduled visit`() {
    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)
    personalRelationshipsApi().stubReferenceGroup()
    personalRelationshipsApi().stubAllApprovedContacts(
      MOORLAND_PRISONER.number,
      pagedModelPrisonerContactSummary(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 123,
          prisonerContactId = 456,
        ),
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 130,
          prisonerContactId = 460,
        ),
      ),
    )
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    webTestClient.updateSlot(
      MOORLAND_PRISONER.prison,
      officialVisitId = scheduledVisit.officialVisitId,
      request = updateSlot,
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_UPDATED,
      additionalInfo = VisitInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitId = scheduledVisit.officialVisitId,
      ),
    )
    val result = officialVisitRepository.findById(scheduledVisit.officialVisitId).get()
    with(result) {
      visitDate isEqualTo visitDateInTheFuture.plusMonths(20)
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 0)
      dpsLocationId isEqualTo location.id
      visitTypeCode isEqualTo VisitType.VIDEO
    }
  }

  @Test
  fun `should update prisoner and staff notes details for an official scheduled visit`() {
    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)
    personalRelationshipsApi().stubAllApprovedContacts(
      MOORLAND_PRISONER.number,
      pagedModelPrisonerContactSummary(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 123,
          prisonerContactId = 456,
        ),
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 130,
          prisonerContactId = 460,
        ),
      ),
    )
    personalRelationshipsApi().stubReferenceGroup()

    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    webTestClient.updateComments(
      MOORLAND_PRISONER.prison,
      officialVisitId = scheduledVisit.officialVisitId,
      request = OfficialVisitUpdateCommentRequest(
        staffNotes = "New staff Notes",
        prisonerNotes = "New prison Notes",
      ),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_UPDATED,
      additionalInfo = VisitInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitId = scheduledVisit.officialVisitId,
      ),
    )
    val result = officialVisitRepository.findById(scheduledVisit.officialVisitId).get()

    with(result) {
      staffNotes isEqualTo "New staff Notes"
      prisonerNotes isEqualTo "New prison Notes"
    }
  }

  @Test
  @Transactional
  fun `should update existing visitors details, add new visitor and remove existing visitor for an official scheduled visit`() {
    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER)
    personalRelationshipsApi().stubAllApprovedContacts(
      MOORLAND_PRISONER.number,
      pagedModelPrisonerContactSummary(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 123,
          prisonerContactId = 456,
        ),
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 124,
          prisonerContactId = 457,
        ),
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 130,
          prisonerContactId = 460,
        ),
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 125,
          prisonerContactId = 458,
        ),
      ),
    )

    personalRelationshipsApi().stubReferenceGroup()
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)
    webTestClient.updateVisitors(
      MOORLAND_PRISONER.prison,
      officialVisitId = scheduledVisit.officialVisitId,
      request = OfficialVisitUpdateVisitorsRequest(
        officialVisitors = listOf(
          // add
          OfficialVisitor(
            visitorTypeCode = VisitorType.CONTACT,
            relationshipCode = "POM1",
            contactId = 125,
            prisonerContactId = 458,
            leadVisitor = true,
            assistedVisit = true,
            assistedNotes = "visitor notes two new updated1",
          ),
          // modify
          OfficialVisitor(
            officialVisitorId = scheduledVisit.officialVisitorIds.first(),
            visitorTypeCode = VisitorType.CONTACT,
            relationshipCode = "POM2",
            contactId = 124,
            prisonerContactId = 457,
            leadVisitor = true,
            assistedVisit = true,
            assistedNotes = "visitor notes two new updated2",
          ),
        ),
      ),
    )
    // Modified ID
    val existingVisitorID = scheduledVisit.officialVisitorIds.first()

    // Deleted ID
    val deletedVisitorId = scheduledVisit.officialVisitorIds.last()

    val result = officialVisitRepository.findById(scheduledVisit.officialVisitId).get()

    // modified.
    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_UPDATED,
      additionalInfo = VisitorInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitorId = existingVisitorID,
        officialVisitId = result.officialVisitId,
      ),
    )
    // removed
    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_DELETED,
      additionalInfo = VisitorInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitorId = deletedVisitorId,
        officialVisitId = result.officialVisitId,
      ),
    )

    // created
    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_CREATED,
      additionalInfo = VisitorInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitorId = scheduledVisit.officialVisitorIds.single { t -> t != existingVisitorID },
        officialVisitId = result.officialVisitId,
      ),
    )
    with(result) {
      officialVisitors().size isEqualTo 2
    }
    val updatedVisitor = officialVisitorRepository.findById(existingVisitorID).get()
    with(updatedVisitor) {
      officialVisitorId isEqualTo scheduledVisit.officialVisitorIds.first()
      visitorTypeCode isEqualTo VisitorType.CONTACT
      relationshipCode isEqualTo "POM2"
      contactId isEqualTo 124
      prisonerContactId isEqualTo 457
      leadVisitor isEqualTo true
      assistedVisit isEqualTo true
      visitorNotes isEqualTo "visitor notes two new updated2"
    }
  }

  private fun WebTestClient.updateSlot(
    prisonCode: String,
    officialVisitId: Long,
    request: OfficialVisitUpdateSlotRequest,
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
  ) = this
    .put()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId/update-type-and-slot")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk

  private fun WebTestClient.updateComments(
    prisonCode: String,
    officialVisitId: Long,
    request: OfficialVisitUpdateCommentRequest,
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
  ) = this
    .put()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId/update-comments")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk

  private fun WebTestClient.updateVisitors(
    prisonCode: String,
    officialVisitId: Long,
    request: OfficialVisitUpdateVisitorsRequest,
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
  ) = this
    .put()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId/visitors")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
}
