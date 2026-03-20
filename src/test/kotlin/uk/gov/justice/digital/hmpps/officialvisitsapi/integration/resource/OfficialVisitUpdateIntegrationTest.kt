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
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateCommentRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.PersonReference
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitorInfo
import java.time.LocalTime

class OfficialVisitUpdateIntegrationTest : IntegrationTestBase() {
  private val location = moorlandLocation

  private val prisonerVisitors = listOf(
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

  private var scheduledVisit: CreateOfficialVisitResponse? = null

  private val nextMondayAt9 = createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, prisonerVisitors)

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
    stubEvents.reset()

    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(prisonCode = MOORLAND, locations = listOf(location))
    locationsInsidePrisonApi().stubGetLocationById(location)
    personalRelationshipsApi().stubReferenceGroup()
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER)
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER_ADDED)
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

    // Before each test, create a visit and reset the stubbed events
    scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9)
    stubEvents.reset()
  }

  @Test
  fun `should update the visit type and slot`() {
    val updateVisitSlotRequest = OfficialVisitUpdateSlotRequest(
      prisonVisitSlotId = 1,
      visitDate = nextMondayAt9.visitDate!!.plusMonths(20),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationId = location.id,
      visitTypeCode = VisitType.VIDEO,
    )

    webTestClient.updateSlot(
      MOORLAND_PRISONER.prison,
      officialVisitId = scheduledVisit?.officialVisitId!!,
      request = updateVisitSlotRequest,
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_UPDATED,
      additionalInfo = VisitInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitId = scheduledVisit?.officialVisitId!!,
      ),
      personReference = PersonReference(
        nomsNumber = MOORLAND_PRISONER.number,
      ),
    )

    val result = officialVisitRepository.findById(scheduledVisit?.officialVisitId!!).get()

    with(result) {
      visitDate isEqualTo nextMondayAt9.visitDate.plusMonths(20)
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 0)
      dpsLocationId isEqualTo location.id
      visitTypeCode isEqualTo VisitType.VIDEO
    }
  }

  @Test
  fun `should update prisoner and staff notes`() {
    val updateVisitCommentRequest = OfficialVisitUpdateCommentRequest(
      staffNotes = "New staff notes",
      prisonerNotes = "New prisoner notes",
    )

    webTestClient.updateComments(
      prisonCode = MOORLAND,
      officialVisitId = scheduledVisit?.officialVisitId!!,
      request = updateVisitCommentRequest,
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_UPDATED,
      additionalInfo = VisitInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitId = scheduledVisit?.officialVisitId!!,
      ),
      personReference = PersonReference(
        nomsNumber = MOORLAND_PRISONER.number,
      ),
    )

    val result = officialVisitRepository.findById(scheduledVisit?.officialVisitId!!).get()

    with(result) {
      staffNotes isEqualTo "New staff notes"
      prisonerNotes isEqualTo "New prisoner notes"
    }
  }

  @Test
  fun `should add, update and delete visitors as requested`() {
    stubEvents.assertNoEvents()

    // Update the visitors - add one, delete one, update one
    webTestClient.updateVisitors(
      MOORLAND_PRISONER.prison,
      officialVisitId = scheduledVisit?.officialVisitId!!,
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
          // Update - contactId = 123 and delete their equipment
          OfficialVisitor(
            officialVisitorId = scheduledVisit?.visitorAndContactIds?.first()?.first!!,
            visitorTypeCode = VisitorType.CONTACT,
            relationshipCode = "POM",
            contactId = scheduledVisit?.visitorAndContactIds?.first()?.second,
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

    val existingVisitorId = scheduledVisit?.visitorAndContactIds?.first()?.first
    val deletedVisitorId = scheduledVisit?.visitorAndContactIds?.last()?.first

    // Get the visit to check the changes have been applied
    val result = webTestClient.getOfficialVisitByPrisonAndId(MOORLAND, scheduledVisit?.officialVisitId!!)

    stubEvents.assertEventsSize(4)

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_UPDATED,
      additionalInfo = VisitInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitId = result.officialVisitId,
      ),
      personReference = PersonReference(
        nomsNumber = MOORLAND_PRISONER.number,
      ),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_UPDATED,
      additionalInfo = VisitorInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitorId = existingVisitorId!!,
        officialVisitId = result.officialVisitId,
      ),
      personReference = PersonReference(
        contactId = scheduledVisit?.visitorAndContactIds?.first()?.second!!,
      ),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_DELETED,
      additionalInfo = VisitorInfo(
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitorId = deletedVisitorId!!,
        officialVisitId = result.officialVisitId,
      ),
      personReference = PersonReference(
        contactId = scheduledVisit?.visitorAndContactIds?.last()?.second!!,
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

    val auditEvents = auditedEventRepository.findAll()

    auditEvents hasSize 2

    auditEvents.single { it.summaryText == "Official visit created" }

    with(auditEvents.single { it.summaryText == "Update visit visitors" }) {
      officialVisitId isEqualTo result.officialVisitId
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      summaryText isEqualTo "Update visit visitors"
      detailText isEqualTo "Visitors added 1; Visitors updated 1; Visitors removed 1."
      userName isEqualTo MOORLAND_PRISON_USER.username
      userFullName isEqualTo MOORLAND_PRISON_USER.name
      eventSource isEqualTo Source.DPS.name
      eventDateTime isCloseTo now()
    }
  }

  @Test
  fun `should fail to update a visit when the visitor ID does not exist`() {
    webTestClient.nonExistingVisitors(
      MOORLAND_PRISONER.prison,
      officialVisitId = scheduledVisit?.officialVisitId!!,
      request = OfficialVisitUpdateVisitorsRequest(
        officialVisitors = listOf(
          OfficialVisitor(
            officialVisitorId = Long.MAX_VALUE,
            visitorTypeCode = VisitorType.CONTACT,
            relationshipCode = "POM",
            contactId = scheduledVisit?.visitorAndContactIds?.first()?.second,
            prisonerContactId = 456,
            leadVisitor = false,
            assistedVisit = true,
            assistedNotes = "visitor notes updated",
            visitorEquipment = VisitorEquipment(""),
          ),
        ),
      ),
    )
  }

  @Test
  fun `should fail to update a visit when the visit ID does not exist`() {
    val updateVisitSlotRequest = OfficialVisitUpdateSlotRequest(
      prisonVisitSlotId = 1,
      visitDate = nextMondayAt9.visitDate!!.plusMonths(20),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationId = location.id,
      visitTypeCode = VisitType.VIDEO,
    )

    webTestClient.notExistentOfficialVisit(
      request = updateVisitSlotRequest,
      prisonCode = MOORLAND_PRISONER.prison,
      officialVisitId = 99,
      prisonUser = MOORLAND_PRISON_USER,
    )
  }

  @Test
  fun `should fail to update comments when the visit ID does not exist`() {
    val updateVisitSlotRequest = OfficialVisitUpdateSlotRequest(
      prisonVisitSlotId = 1,
      visitDate = nextMondayAt9.visitDate!!.plusMonths(20),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationId = location.id,
      visitTypeCode = VisitType.VIDEO,
    )

    webTestClient.notExistentOfficialVisit(
      request = updateVisitSlotRequest,
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

  private fun WebTestClient.nonExistingVisitors(
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
    .expectStatus().is4xxClientError
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody().jsonPath("$.userMessage").isEqualTo("Request contains visitors which do not exist on official visit with id $officialVisitId")

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
