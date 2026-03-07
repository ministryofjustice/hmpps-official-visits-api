package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER_ADDED
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
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.PersonReference
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitorInfo
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class OfficialVisitUpdateIntegrationTest : IntegrationTestBase() {
  private val location = moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))

  private val listOfOfficialVisitors = listOf(
    OfficialVisitor(
      visitorTypeCode = VisitorType.CONTACT,
      relationshipCode = "FRI",
      contactId = 123,
      prisonerContactId = 456,
      leadVisitor = true,
      assistedVisit = true,
      assistedNotes = "visitor 1 notes",
      visitorEquipment = VisitorEquipment("Bringing secure laptop"),
    ),
    OfficialVisitor(
      visitorTypeCode = VisitorType.CONTACT,
      relationshipCode = "FRI",
      contactId = 130,
      prisonerContactId = 460,
      leadVisitor = false,
      assistedVisit = true,
      assistedNotes = "visitor 2 notes",
      visitorEquipment = VisitorEquipment("Bringing phone"),
    ),
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
    officialVisitors = listOfOfficialVisitors,
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
  fun `should update the visit type and slot`() {
    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)

    personalRelationshipsApi().stubReferenceGroup()

    // TODO: For the create request - only needs approved contacts - update to use new method (include inactive) when create changes made
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
      personReference = PersonReference(
        nomsNumber = MOORLAND_PRISONER.number,
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
  fun `should update prisoner and staff notes`() {
    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)

    // TODO: For the create request - only needs approved contacts - up to include active and inactive when create changes made
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
        staffNotes = "New staff notes",
        prisonerNotes = "New prisoner notes",
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
      personReference = PersonReference(
        nomsNumber = MOORLAND_PRISONER.number,
      ),
    )

    val result = officialVisitRepository.findById(scheduledVisit.officialVisitId).get()

    with(result) {
      staffNotes isEqualTo "New staff notes"
      prisonerNotes isEqualTo "New prisoner notes"
    }
  }

  @Test
  fun `should add, update and delete visitors as requested`() {
    personalRelationshipsApi().stubReferenceGroup()
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER)
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER_ADDED)

    // TODO: For the create request - which only needs approved contacts - change this to the new method to include inactive contacts when updating the create transaction
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

    stubEvents.reset()

    // For the update request - which needs all contacts (not just the active ones)- can be used when create journey updated
    personalRelationshipsApi().stubAllContacts(
      MOORLAND_PRISONER.number,
      listOf(
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

    webTestClient.updateVisitors(
      MOORLAND_PRISONER.prison,
      officialVisitId = scheduledVisit.officialVisitId,
      request = OfficialVisitUpdateVisitorsRequest(
        officialVisitors = listOf(
          // Add - contactId = 125
          OfficialVisitor(
            visitorTypeCode = VisitorType.CONTACT,
            relationshipCode = "POM",
            contactId = 125,
            prisonerContactId = 458,
            leadVisitor = true,
            assistedVisit = true,
            assistedNotes = "visitor notes added",
            visitorEquipment = VisitorEquipment("Bringing secure laptop"),
          ),
          // Update - contactId = 123 and delete equipment
          OfficialVisitor(
            officialVisitorId = scheduledVisit.visitorAndContactIds.first().first,
            visitorTypeCode = VisitorType.CONTACT,
            relationshipCode = "POM",
            contactId = scheduledVisit.visitorAndContactIds.first().second,
            prisonerContactId = 456,
            leadVisitor = false,
            assistedVisit = true,
            assistedNotes = "visitor notes updated",
            visitorEquipment = VisitorEquipment(""),
          ),
          // Delete - contactId = 130 (which existed in the earlier create)
        ),
      ),
    )

    // The order in which operations are done is important - i.e. the additions, updates and deletes
    val existingVisitorId = scheduledVisit.visitorAndContactIds.first().first
    val deletedVisitorId = scheduledVisit.visitorAndContactIds.last().first

    // Get the visit to check the changes have been applied
    val result = webTestClient.getOfficialVisitByPrisonAndId(MOORLAND, scheduledVisit.officialVisitId)

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_UPDATED,
      additionalInfo = VisitorInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitorId = existingVisitorId,
        officialVisitId = result.officialVisitId,
      ),
      personReference = PersonReference(
        contactId = scheduledVisit.visitorAndContactIds.first().second!!,
      ),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_DELETED,
      additionalInfo = VisitorInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitorId = deletedVisitorId,
        officialVisitId = result.officialVisitId,
      ),
      personReference = PersonReference(
        // contactId = nextMondayAt9.officialVisitors.last().contactId!!,
        contactId = scheduledVisit.visitorAndContactIds.last().second!!,
      ),
    )

    // The created visitor must be the one which does not match the updated visitor ID
    val createdVisitor = result.officialVisitors?.filter { visitor -> visitor.officialVisitorId != existingVisitorId }

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_CREATED,
      additionalInfo = VisitorInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitorId = createdVisitor!!.last().officialVisitorId,
        officialVisitId = result.officialVisitId,
      ),
      personReference = PersonReference(
        contactId = createdVisitor.last().contactId!!,
      ),
    )

    with(result) {
      officialVisitors?.size isEqualTo 2
    }

    val updatedVisitor = result.officialVisitors.filter { visitor -> visitor.officialVisitorId == existingVisitorId }

    // Check that the changes to the existing visitor have been applied
    with(updatedVisitor[0]) {
      officialVisitorId isEqualTo updatedVisitor.first().officialVisitorId
      visitorTypeCode isEqualTo VisitorType.CONTACT
      relationshipCode isEqualTo "FRI" // From stubbed PR API response
      contactId isEqualTo 123
      prisonerContactId isEqualTo 456
      leadVisitor isEqualTo false
      assistedVisit isEqualTo true
      visitorNotes isEqualTo "visitor notes updated"
      visitorEquipment isEqualTo null
    }
  }

  @Test
  fun `should fail to update visit when the visit ID does not exist`() {
    webTestClient.notExistentOfficialVisit(
      request = updateSlot,
      prisonCode = MOORLAND_PRISONER.prison,
      officialVisitId = 99,
      prisonUser = MOORLAND_PRISON_USER,
    )
  }

  @Test
  fun `should fail to update comments when the visit ID does not exist`() {
    webTestClient.notExistentOfficialVisit(
      request = updateSlot,
      prisonCode = MOORLAND_PRISONER.prison,
      officialVisitId = 99,
      prisonUser = MOORLAND_PRISON_USER,
    )
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

  private fun WebTestClient.notExistentOfficialVisit(
    request: OfficialVisitUpdateSlotRequest,
    prisonCode: String,
    officialVisitId: Long,
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
  ) = this
    .put()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId/update-type-and-slot")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN"))).exchange()
    .expectStatus().is4xxClientError
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody().jsonPath("$.userMessage").isEqualTo("Official visit with id $officialVisitId and prison code $prisonCode not found")

  private fun WebTestClient.getOfficialVisitByPrisonAndId(prisonCode: String, officialVisitId: Long) = this
    .get()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<OfficialVisitDetails>()
    .returnResult().responseBody!!
}
