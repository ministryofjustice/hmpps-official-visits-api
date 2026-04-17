package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.ErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.DuplicateOffenderVisitIdErrorResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.PersonReference
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitorInfo
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class SyncOfficialVisitIntegrationTest : IntegrationTestBase() {
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

    // Stub client calls for manage users, personal relationships, contacts and locations
    manageUsersApi().stubGetUserDetails(MOORLAND_PRISON_USER.username, AuthSource.nomis, MOORLAND_PRISON_USER.name, MOORLAND, MOORLAND_PRISON_USER.username)

    // Stub a known contact
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = 123,
          prisonerContactId = 456,
        ),
      ),
    )

    // TODO: Do we need this?
    personalRelationshipsApi().stubPrisonerContactRelationships(MOORLAND_PRISONER.number, 2L)

    // Stub locations for visits
    locationsInsidePrisonApi().stubGetLocationById(moorlandLocation)
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(MOORLAND, listOf(moorlandLocation))
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `get a visit - should return an existing visit by ID`() {
    val savedOfficialVisitId = (testAPIClient.createOfficialVisit(officialVisitRequest, MOORLAND_PRISON_USER)).officialVisitId
    stubEvents.reset()

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
  fun `get a visit - should fail when the ID does not exist`() {
    webTestClient.get()
      .uri("/sync/official-visit/id/{officialVisitId}", 999)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody().jsonPath("$.userMessage").isEqualTo("Official visit with id 999 not found")
  }

  @Test
  fun `create a visit - should create a visit with no visitors`() {
    val request = syncCreateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    )

    val response = webTestClient.syncCreateOfficialVisit(request)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncOfficialVisit>()
      .returnResult().responseBody!!

    assertThat(response.dpsLocationId).isEqualTo(request.dpsLocationId)
    assertThat(response.visitComments).isEqualTo(request.commentText)
    assertThat(response.statusCode).isEqualTo(VisitStatusType.SCHEDULED)
    assertThat(response.visitType).isEqualTo(VisitType.UNKNOWN)
    assertThat(response.officialVisitId).isGreaterThan(0L)
    assertThat(response.offenderVisitId).isEqualTo(request.offenderVisitId)
    assertThat(response.prisonCode).isEqualTo(request.prisonCode)
    assertThat(response.prisonerNumber).isEqualTo(request.prisonerNumber)
    assertThat(response.visitDate).isEqualTo(request.visitDate)
    assertThat(response.startTime).isEqualTo(request.startTime)
    assertThat(response.endTime).isEqualTo(request.endTime)
    assertThat(response.currentTerm).isEqualTo(request.currentTerm)

    assertThat(response.visitors).isEmpty()

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_CREATED,
      additionalInfo = VisitInfo(
        source = Source.NOMIS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitId = response.officialVisitId,
      ),
      personReference = PersonReference(nomsNumber = response.prisonerNumber),
    )
  }

  @Test
  fun `create a visit - should fail when the visit slot does not exist`() {
    val request = syncCreateOfficialVisitRequest(
      offenderVisitId = 2L,
      prisonVisitSlotId = 9999L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    )

    val response = webTestClient.syncCreateOfficialVisit(request)
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<ErrorResponse>()
      .returnResult().responseBody!!

    assertThat(response.userMessage).isEqualTo("Prison visit slot ID 9999 does not exist")

    stubEvents.assertHasNoEvents(OutboundEvent.VISIT_CREATED)
  }

  @Test
  fun `create a visit - should fail due to duplicate offenderVisitId`() {
    val request = syncCreateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    )

    // First request should succeed
    val response1 = webTestClient.syncCreateOfficialVisit(request)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncOfficialVisit>()
      .returnResult().responseBody!!

    stubEvents.reset()

    // Second request for the same offenderVisitId should fail
    val response2 = webTestClient.syncCreateOfficialVisit(request)
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<DuplicateOffenderVisitIdErrorResponse>()
      .returnResult().responseBody!!

    assertThat(response2.message).isEqualTo("Official visit with offenderVisitId ${request.offenderVisitId} already exists (DPS ID ${response1.officialVisitId})")

    stubEvents.assertHasNoEvents(OutboundEvent.VISIT_CREATED)
  }

  @Test
  fun `update a visit - should update a visit`() {
    val request = syncCreateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    )

    val createResponse = webTestClient.syncCreateOfficialVisit(request)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncOfficialVisit>()
      .returnResult().responseBody!!

    stubEvents.reset()

    val updateRequest = syncUpdateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    )

    val updateResponse = webTestClient.syncUpdateOfficialVisit(updateRequest, createResponse.officialVisitId)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncOfficialVisit>()
      .returnResult().responseBody!!

    // Check the updated values
    assertThat(updateResponse.visitComments).isEqualTo(updateRequest.commentText)
    assertThat(updateResponse.visitorConcernNotes).isEqualTo(updateRequest.visitorConcernText)
    assertThat(updateResponse.statusCode).isEqualTo(updateRequest.visitStatusCode)
    assertThat(updateResponse.startTime).isEqualTo(updateRequest.startTime)
    assertThat(updateResponse.endTime).isEqualTo(updateRequest.endTime)

    // Check the other values remain unchanged
    assertThat(updateResponse.officialVisitId).isEqualTo(createResponse.officialVisitId)
    assertThat(updateResponse.visitType).isEqualTo(createResponse.visitType)
    assertThat(updateResponse.prisonCode).isEqualTo(createResponse.prisonCode)
    assertThat(updateResponse.prisonerNumber).isEqualTo(createResponse.prisonerNumber)
    assertThat(updateResponse.visitDate).isEqualTo(createResponse.visitDate)
    assertThat(updateResponse.currentTerm).isEqualTo(request.currentTerm)

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_UPDATED,
      additionalInfo = VisitInfo(
        source = Source.NOMIS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
        officialVisitId = updateResponse.officialVisitId,
      ),
      personReference = PersonReference(nomsNumber = updateResponse.prisonerNumber),
    )
  }

  @Test
  fun `update a visit - should fail when the visit does not exist`() {
    val nonExistentVisitId = 99L

    val request = syncUpdateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    )

    val response = webTestClient.syncUpdateOfficialVisit(request, nonExistentVisitId)
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<ErrorResponse>()
      .returnResult().responseBody!!

    assertThat(response.userMessage).isEqualTo("Official visit with ID $nonExistentVisitId not found")

    stubEvents.assertHasNoEvents(OutboundEvent.VISIT_UPDATED)
  }

  @Test
  fun `update a visit - should fail when the requested visit slot does not exist`() {
    val nonExistentVisitSlotId = 999L

    val request = syncCreateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    )

    val createResponse = webTestClient.syncCreateOfficialVisit(request)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncOfficialVisit>()
      .returnResult().responseBody!!

    stubEvents.reset()

    // Override the visit slot ID to an unknown slot
    val updateRequest = syncUpdateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    ).copy(prisonVisitSlotId = nonExistentVisitSlotId)

    val response = webTestClient.syncUpdateOfficialVisit(updateRequest, createResponse.officialVisitId)
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<ErrorResponse>()
      .returnResult().responseBody!!

    assertThat(response.userMessage).isEqualTo("Visit slot with ID $nonExistentVisitSlotId not found")

    stubEvents.assertHasNoEvents(OutboundEvent.VISIT_UPDATED)
  }

  @Test
  fun `delete a visit - should silently succeed when the ID is not found`() {
    webTestClient.delete(99L)

    stubEvents.assertHasNoEvents(event = OutboundEvent.VISIT_DELETED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.VISITOR_DELETED)
  }

  @Test
  fun `delete a visit - should delete a visit by its ID`() {
    val officialVisit = webTestClient.createOfficialVisit(officialVisitRequest)
    stubEvents.reset()

    webTestClient.delete(officialVisit.officialVisitId)

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_DELETED,
      additionalInfo = VisitorInfo(
        officialVisitId = officialVisit.officialVisitId,
        officialVisitorId = officialVisit.visitorAndContactIds.first().first,
        source = Source.NOMIS,
        username = UserService.getClientAsUser("NOMIS").username,
        prisonId = MOORLAND,
      ),
      PersonReference(
        contactId = officialVisit.visitorAndContactIds.first().second ?: 0L,
      ),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_DELETED,
      additionalInfo = VisitInfo(
        officialVisitId = officialVisit.officialVisitId,
        source = Source.NOMIS,
        username = UserService.getClientAsUser("NOMIS").username,
        prisonId = MOORLAND,
      ),
      PersonReference(
        nomsNumber = officialVisit.prisonerNumber,
      ),
    )

    val deleteAudit = auditedEventRepository.findAll().single { it.summaryText == "Official visit deleted" }
    assertThat(deleteAudit.officialVisitId).isEqualTo(officialVisit.officialVisitId)
    assertThat(deleteAudit.prisonCode).isEqualTo(MOORLAND)
    assertThat(deleteAudit.prisonerNumber).isEqualTo(officialVisit.prisonerNumber)
    assertThat(deleteAudit.eventSource).isEqualTo("NOMIS")
    assertThat(deleteAudit.userName).isEqualTo("NOMIS")
    assertThat(deleteAudit.userFullName).isEqualTo("NOMIS")
    assertThat(deleteAudit.detailText).isEqualTo("Official visit deleted for prisoner number ${officialVisit.prisonerNumber}")
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
    assertThat(currentTerm).isEqualTo(true)
    assertThat(visitors.size).isEqualTo(request.officialVisitors.size)

    // Visitor attributes
    assertThat(visitors.first().leadVisitor).isEqualTo(request.officialVisitors.first().leadVisitor)
    assertThat(visitors.first().contactId).isEqualTo(request.officialVisitors.first().contactId)
    assertThat(visitors.first().assistedVisit).isEqualTo(request.officialVisitors.first().assistedVisit)
    assertThat(visitors.first().visitorNotes).isEqualTo(request.officialVisitors.first().assistedNotes)
  }

  private fun syncCreateOfficialVisitRequest(
    offenderVisitId: Long,
    prisonVisitSlotId: Long,
    dpsLocationId: UUID,
  ) = SyncCreateOfficialVisitRequest(
    offenderVisitId = offenderVisitId,
    prisonVisitSlotId = prisonVisitSlotId,
    offenderBookId = MOORLAND_PRISONER.bookingId,
    prisonCode = MOORLAND,
    prisonerNumber = MOORLAND_PRISONER.number,
    currentTerm = true,
    visitDate = visitDateInTheFuture,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = dpsLocationId,
    commentText = "comment text",
    visitorConcernText = "visitor concern",
    visitOrderNumber = 1234,
    createUsername = MOORLAND_PRISON_USER.username,
    createDateTime = LocalDateTime.now().minusMinutes(10),
  )

  private fun syncUpdateOfficialVisitRequest(
    offenderVisitId: Long,
    prisonVisitSlotId: Long,
    dpsLocationId: UUID,
  ) = SyncUpdateOfficialVisitRequest(
    offenderVisitId = offenderVisitId,
    prisonVisitSlotId = prisonVisitSlotId,
    offenderBookId = MOORLAND_PRISONER.bookingId,
    prisonCode = MOORLAND,
    prisonerNumber = MOORLAND_PRISONER.number,
    visitDate = visitDateInTheFuture,
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    dpsLocationId = dpsLocationId,
    visitStatusCode = VisitStatusType.EXPIRED,
    commentText = "updated comment",
    visitorConcernText = "updated concern",
    visitOrderNumber = 5678,
    updateUsername = MOORLAND_PRISON_USER.username,
    updateDateTime = LocalDateTime.now().minusMinutes(5),
  )

  private fun WebTestClient.syncCreateOfficialVisit(request: SyncCreateOfficialVisitRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .post()
    .uri("/sync/official-visit")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_MIGRATION")))
    .exchange()

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

  private fun WebTestClient.syncUpdateOfficialVisit(request: SyncUpdateOfficialVisitRequest, officialVisitId: Long, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .put()
    .uri("/sync/official-visit/{officialVisitId}", officialVisitId)
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_MIGRATION")))
    .exchange()

  private fun WebTestClient.delete(officialVisitId: Long) = this
    .delete()
    .uri("/sync/official-visit/id/{officialVisitId}", officialVisitId)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .exchange()
    .expectStatus()
    .is2xxSuccessful
}
