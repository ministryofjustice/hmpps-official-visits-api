package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.PersonReference
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitorInfo
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class SyncOfficialVisitIntegrationTest : IntegrationTestBase() {
  private var savedOfficialVisitId = 0L

  private final val visitDateInTheFuture = today().next(DayOfWeek.MONDAY)

  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = CONTACT_MOORLAND_PRISONER.contactId,
    prisonerContactId = CONTACT_MOORLAND_PRISONER.prisonerContactId,
    leadVisitor = true,
    assistedVisit = false,
    assistedNotes = "visitor notes",
    visitorEquipment = VisitorEquipment("Bringing secure laptop"),
  )

  private val officialVisitRequest = CreateOfficialVisitRequest(
    prisonerNumber = MOORLAND_PRISONER.number,
    prisonVisitSlotId = 1,
    visitDate = visitDateInTheFuture,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    visitTypeCode = VisitType.IN_PERSON,
    staffNotes = "private notes",
    prisonerNotes = "public notes",
    officialVisitors = listOf(officialVisitor),
  )

  @BeforeEach
  @Transactional
  fun initialiseData() {
    clearAllVisitData()

    // Stub client calls for approved contacts and locations
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)
    locationsInsidePrisonApi().stubGetLocationById(moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")))
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(
      MOORLAND,
      listOf(
        moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")),
      ),
    )

    // Create an official visit to use with each test
    savedOfficialVisitId = (webTestClient.createOfficialVisit(officialVisitRequest)).officialVisitId
    stubEvents.reset()
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should get an existing official visit by ID`() {
    val syncOfficialVisit = webTestClient.get()
      .uri("/sync/official-visit/id/{officialVisitId}", savedOfficialVisitId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncOfficialVisit>()
      .returnResult().responseBody!!

    syncOfficialVisit.assertWithCreateRequest(officialVisitRequest)
  }

  @Test
  fun `should get success response for the deletion of non existing official visit ID`() {
    webTestClient.delete()
      .uri("/sync/official-visit/id/{officialVisitId}", 99)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .is2xxSuccessful

    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_DELETED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.VISITOR_DELETED)
  }

  @Test
  fun `should delete official visit by ID`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)
    locationsInsidePrisonApi().stubGetLocationById(moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")))
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(
      MOORLAND,
      listOf(
        moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")),
      ),
    )

    // Create an official visit to use with each test
    val officialVisit = (webTestClient.createOfficialVisit(officialVisitRequest))

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_CREATED,
      additionalInfo = VisitInfo(officialVisit.officialVisitId, Source.DPS, MOORLAND_PRISON_USER.username, MOORLAND_PRISON_USER.activeCaseLoadId),
      personReference = PersonReference(nomsNumber = MOORLAND_PRISONER.number),
    )
    webTestClient.delete()
      .uri("/sync/official-visit/id/{officialVisitId}", officialVisit.officialVisitId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .is2xxSuccessful

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_DELETED,
      additionalInfo = VisitorInfo(
        officialVisitId = officialVisit.officialVisitId,
        officialVisitorId = officialVisit.officialVisitorIds.first(),
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_DELETED,
      additionalInfo = VisitInfo(
        officialVisitId = officialVisit.officialVisitId,
        source = Source.NOMIS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )
  }

  private fun SyncOfficialVisit.assertWithCreateRequest(request: CreateOfficialVisitRequest) {
    // Main visit attributes
    assertThat(prisonCode).isEqualTo(MOORLAND)
    assertThat(prisonerNumber).isEqualTo(request.prisonerNumber)
    assertThat(visitDate).isEqualTo(request.visitDate)
    assertThat(startTime).isEqualTo(request.startTime)
    assertThat(endTime).isEqualTo(request.endTime)
    assertThat(dpsLocationId).isEqualTo(request.dpsLocationId)
    assertThat(visitType).isEqualTo(request.visitTypeCode)
    assertThat(visitComments).isEqualTo(request.prisonerNotes)
    assertThat(visitors.size).isEqualTo(request.officialVisitors.size)

    // Visitor attributes
    assertThat(visitors.first().leadVisitor).isEqualTo(request.officialVisitors.first().leadVisitor)
    assertThat(visitors.first().contactId).isEqualTo(request.officialVisitors.first().contactId)
    assertThat(visitors.first().assistedVisit).isEqualTo(request.officialVisitors.first().assistedVisit)
    assertThat(visitors.first().visitorNotes).isEqualTo(request.officialVisitors.first().assistedNotes)
  }

  @Test
  fun `should fail to get visit when the ID does not exist`() {
    webTestClient.get()
      .uri("/sync/official-visit/id/{officialVisitId}", 99)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody().jsonPath("$.userMessage").isEqualTo("Official visit with id 99 not found")
  }

  private fun WebTestClient.createOfficialVisit(request: CreateOfficialVisitRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .post()
    .uri("/official-visit/prison/${prisonUser.activeCaseLoadId}")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<CreateOfficialVisitResponse>()
    .returnResult().responseBody!!
}
