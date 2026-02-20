package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isNotEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.PersonReference
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitInfo
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class CreateOfficialVisitIntegrationTest : IntegrationTestBase() {

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
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    visitTypeCode = VisitType.IN_PERSON,
    staffNotes = "private notes",
    prisonerNotes = "public notes",
    searchTypeCode = SearchLevelType.PAT,
    officialVisitors = listOf(officialVisitor),
  )

  private final val nextFridayAt11 = nextMondayAt9.copy(
    visitDate = visitDateInTheFuture.next(DayOfWeek.FRIDAY),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(12, 0),
    prisonVisitSlotId = 9,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
  )

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  @Transactional
  fun `should create official unassisted visit with one social visitor and equipment`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)

    val officialVisitResponse = webTestClient.create(nextMondayAt9)
    val persistedOfficialVisit = officialVisitRepository.findById(officialVisitResponse.officialVisitId).get()

    with(persistedOfficialVisit) {
      prisonCode isEqualTo MOORLAND_PRISONER.prison
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      offenderBookId isEqualTo MOORLAND_PRISONER.bookingId
      prisonVisitSlot.prisonVisitSlotId isEqualTo 1
      visitDate isEqualTo visitDateInTheFuture
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
      dpsLocationId isNotEqualTo null
      visitTypeCode isEqualTo VisitType.IN_PERSON
      visitStatusCode isEqualTo VisitStatusType.SCHEDULED
      staffNotes isEqualTo "private notes"
      prisonerNotes isEqualTo "public notes"
      visitorConcernNotes isEqualTo null
      createdBy isEqualTo MOORLAND_PRISON_USER.username
      createdTime isCloseTo now()
    }

    with(persistedOfficialVisit.officialVisitors().single()) {
      visitorTypeCode isEqualTo VisitorType.CONTACT
      relationshipTypeCode isEqualTo RelationshipType.OFFICIAL
      relationshipCode isEqualTo "POM"
      firstName isEqualTo "John"
      lastName isEqualTo "Doe"
      leadVisitor isEqualTo true
      assistedVisit isEqualTo false
      visitorNotes isEqualTo "visitor notes"
      visitorEquipment!!.description isEqualTo "Bringing secure laptop"
    }

    with(prisonerVisitedRepository.findAll().single { it.officialVisit == persistedOfficialVisit }) {
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      createdBy isEqualTo MOORLAND_PRISON_USER.username
      createdTime isCloseTo now()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_CREATED,
      additionalInfo = VisitInfo(persistedOfficialVisit.officialVisitId, Source.DPS, MOORLAND_PRISON_USER.username, MOORLAND_PRISON_USER.activeCaseLoadId),
      personReference = PersonReference(nomsNumber = MOORLAND_PRISONER.number),
    )
  }

  @Test
  @Transactional
  fun `should create official assisted visit with one social visitor and no equipment`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)

    val officialVisitResponse = webTestClient.create(nextMondayAt9.copy(officialVisitors = listOf(officialVisitor.copy(assistedVisit = true, visitorEquipment = null))))
    val persistedOfficialVisit = officialVisitRepository.findById(officialVisitResponse.officialVisitId).get()

    with(persistedOfficialVisit) {
      prisonCode isEqualTo MOORLAND_PRISONER.prison
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      offenderBookId isEqualTo MOORLAND_PRISONER.bookingId
      prisonVisitSlot.prisonVisitSlotId isEqualTo 1
      visitDate isEqualTo visitDateInTheFuture
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
      dpsLocationId isNotEqualTo null
      visitTypeCode isEqualTo VisitType.IN_PERSON
      visitStatusCode isEqualTo VisitStatusType.SCHEDULED
      staffNotes isEqualTo "private notes"
      prisonerNotes isEqualTo "public notes"
      visitorConcernNotes isEqualTo null
      createdBy isEqualTo MOORLAND_PRISON_USER.username
      createdTime isCloseTo now()
    }

    with(persistedOfficialVisit.officialVisitors().single()) {
      visitorTypeCode isEqualTo VisitorType.CONTACT
      relationshipTypeCode isEqualTo RelationshipType.OFFICIAL
      relationshipCode isEqualTo "POM"
      firstName isEqualTo "John"
      lastName isEqualTo "Doe"
      leadVisitor isEqualTo true
      assistedVisit isEqualTo true
      visitorNotes isEqualTo "visitor notes"
      visitorEquipment isEqualTo null
    }

    with(prisonerVisitedRepository.findAll().single { it.officialVisit == persistedOfficialVisit }) {
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      createdBy isEqualTo MOORLAND_PRISON_USER.username
      createdTime isCloseTo now()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_CREATED,
      additionalInfo = VisitInfo(persistedOfficialVisit.officialVisitId, Source.DPS, MOORLAND_PRISON_USER.username, MOORLAND_PRISON_USER.activeCaseLoadId),
      personReference = PersonReference(nomsNumber = MOORLAND_PRISONER.number),
    )
  }

  @Test
  fun `should fail to create official visit when no matching contact found`() {
    webTestClient.badRequest(nextMondayAt9.copy(officialVisitors = listOf(officialVisitor.copy(contactId = 999))), "Visitor with contact ID 999 and prisoner contact ID 456 is not approved for visiting prisoner number ${MOORLAND_PRISONER.number}.")

    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_CREATED)
  }

  @Test
  fun `should fail to create official visit when slot is no longer available`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)

    // Create a visit and ignore/reset the stubbed event
    webTestClient.create(nextFridayAt11)
    stubEvents.reset()

    // Create another visit at the same time
    webTestClient.badRequest(nextFridayAt11, "Prison visit slot 9 is no longer available for the requested date and time.")
    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_CREATED)
  }

  @Test
  fun `should fail when no official visitors are provided`() {
    webTestClient.badRequest(nextMondayAt9.copy(officialVisitors = emptyList()), "At least one official visitor must be supplied.")
    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_CREATED)
  }

  @Test
  fun `should fail when unknown prison visit slot id`() {
    webTestClient.badRequest(nextMondayAt9.copy(prisonVisitSlotId = -99), "Prison visit slot with id -99 not found.")
    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_CREATED)
  }

  @Test
  fun `should fail when visit date and time is in the past`() {
    val beforeNow = now().minusMinutes(1)

    webTestClient.badRequest(nextMondayAt9.copy(visitDate = beforeNow.toLocalDate(), startTime = beforeNow.toLocalTime()), "Official visit cannot be scheduled in the past")
    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_CREATED)
  }

  @Test
  fun `should fail when visit start time is not before the end time`() {
    webTestClient.badRequest(nextMondayAt9.copy(startTime = nextMondayAt9.endTime), "Official visit start time must be before end time")
    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_CREATED)
  }

  @Test
  fun `should fail when prisoner not at prison`() {
    stubUser(PENTONVILLE_PRISON_USER)

    webTestClient.badRequest(nextMondayAt9, "Prisoner ${MOORLAND_PRISONER.number} not found at prison $PENTONVILLE", PENTONVILLE_PRISON_USER)
    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_CREATED)
  }

  @Test
  fun `should fail to create visit when the prison is not the active caseload for the user`() {
    stubUser(PENTONVILLE_PRISON_USER)

    webTestClient.conflict(
      prisonCode = MOORLAND,
      prisonUser = PENTONVILLE_PRISON_USER,
      request = nextMondayAt9,
      errorMessage = "This visit cannot be created in a prison which is not the active caseload for the user",
    )

    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_CREATED)
  }

  private fun WebTestClient.create(request: CreateOfficialVisitRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .post()
    .uri("/official-visit/prison/${prisonUser.activeCaseLoadId}")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(CreateOfficialVisitResponse::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.badRequest(request: CreateOfficialVisitRequest, errorMessage: String, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .post()
    .uri("/official-visit/prison/${prisonUser.activeCaseLoadId}")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isBadRequest
    .expectBody().jsonPath("$.userMessage").isEqualTo(errorMessage)

  private fun WebTestClient.conflict(request: CreateOfficialVisitRequest, errorMessage: String, prisonUser: PrisonUser = MOORLAND_PRISON_USER, prisonCode: String) = this
    .post()
    .uri("/official-visit/prison/$prisonCode")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().value(equalTo(HttpStatus.CONFLICT.value()))
    .expectBody().jsonPath("$.userMessage").isEqualTo(errorMessage)
}
