package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncVisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitSlotInfo
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class SyncVisitSlotIntegrationTest : IntegrationTestBase() {
  @Autowired
  lateinit var prisonVisitSlotRepository: PrisonVisitSlotRepository
  private var savedPrisonVisitSlotId = 0L

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  private final val visitDateInTheFuture = today().next(DayOfWeek.MONDAY)

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
    searchTypeCode = SearchLevelType.PAT,
    officialVisitors = listOf(officialVisitor),
  )

  @BeforeEach
  fun initialiseData() {
    savedPrisonVisitSlotId = (webTestClient.createVisitSlot()).visitSlotId
    stubEvents.reset()
  }

  @Test
  fun `should create a new prison visit slot`() {
    val syncVisitSlot = webTestClient.createVisitSlot()

    syncVisitSlot.assertWithCreateRequest(createVisitSlotRequest())

    assertThat(syncVisitSlot.visitSlotId).isGreaterThan(0)

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_SLOT_CREATED,
      additionalInfo = VisitSlotInfo(
        visitSlotId = syncVisitSlot.visitSlotId,
        source = Source.NOMIS,
        username = "OFFICIAL_VISITS_SERVICE",
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should update an existing visit slot`() {
    val updateRequest = updateVisitSlotRequest()

    val syncVisitSlot = webTestClient.put()
      .uri("/sync/visit-slot/{prisonVisitSlotId}", savedPrisonVisitSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncVisitSlot>()
      .returnResult().responseBody!!

    assertThat(syncVisitSlot.visitSlotId).isEqualTo(savedPrisonVisitSlotId)
    assertThat(syncVisitSlot.maxAdults).isEqualTo(15)

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_SLOT_UPDATED,
      additionalInfo = VisitSlotInfo(
        visitSlotId = syncVisitSlot.visitSlotId,
        source = Source.NOMIS,
        username = "OFFICIAL_VISITS_SERVICE",
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should get an existing visit slot by ID`() {
    val syncVisitSlot = webTestClient.get()
      .uri("/sync/visit-slot/{prisonVisitSlotId}", savedPrisonVisitSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncVisitSlot>()
      .returnResult().responseBody!!

    syncVisitSlot.assertWithCreateRequest(createVisitSlotRequest())
    assertThat(syncVisitSlot.visitSlotId).isGreaterThan(0)
  }

  @Test
  fun `should fail to delete visit slot which does not exist`() {
    webTestClient.delete()
      .uri("/sync/visit-slot/{prisonVisitSlotId}", 99)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody().jsonPath("$.userMessage").isEqualTo("Prison visit slot with ID 99 was not found")
  }

  @Test
  fun `should delete visit slot if there are no associated official visits`() {
    val syncVisitSlot = webTestClient.createVisitSlot()

    webTestClient.delete()
      .uri("/sync/visit-slot/{prisonVisitSlotId}", syncVisitSlot.visitSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .is2xxSuccessful
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncVisitSlot>()

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_SLOT_CREATED,
      additionalInfo = VisitSlotInfo(
        visitSlotId = syncVisitSlot.visitSlotId,
        source = Source.NOMIS,
        username = "OFFICIAL_VISITS_SERVICE",
        prisonId = MOORLAND,
      ),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_SLOT_DELETED,
      additionalInfo = VisitSlotInfo(
        visitSlotId = syncVisitSlot.visitSlotId,
        source = Source.NOMIS,
        username = "OFFICIAL_VISITS_SERVICE",
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should fail to delete visit slot which has associated official visit`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)
    webTestClient.createOfficialVisit(officialVisitRequest)
    webTestClient.delete()
      .uri("/sync/visit-slot/{prisonVisitSlotId}", 1)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody().jsonPath("$.userMessage").isEqualTo("The prison visit slot has visits associated with it and cannot be deleted.")
  }

  private fun SyncVisitSlot.assertWithCreateRequest(request: SyncCreateVisitSlotRequest) {
    assertThat(maxAdults).isEqualTo(request.maxAdults)
    assertThat(dpsLocationId).isEqualTo(request.dpsLocationId)
    assertThat(createdBy).isEqualTo(request.createdBy)
    assertThat(createdTime).isCloseTo(request.createdTime, within(2, ChronoUnit.SECONDS))
  }

  private fun createVisitSlotRequest() = SyncCreateVisitSlotRequest(
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    createdBy = "Test",
    createdTime = createdTime,
  )

  private fun updateVisitSlotRequest() = SyncUpdateVisitSlotRequest(
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    updatedBy = "Test",
    maxAdults = 15,
    updatedTime = updatedTime,
  )

  fun WebTestClient.createVisitSlot() = this.post()
    .uri("/sync/visit-slot")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .bodyValue(createVisitSlotRequest())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<SyncVisitSlot>()
    .returnResult().responseBody!!

  private fun WebTestClient.createOfficialVisit(request: CreateOfficialVisitRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
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
}
