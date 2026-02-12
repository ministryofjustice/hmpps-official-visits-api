package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitorInfo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class SyncOfficialVisitorsIntegrationTest : IntegrationTestBase() {
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
    manageUsersApi().stubGetUserDetails(
      MOORLAND_PRISON_USER.username,
      AuthSource.nomis,
      MOORLAND_PRISON_USER.name,
      MOORLAND,
      MOORLAND_PRISON_USER.username,
    )
    personalRelationshipsApi().stubAllApprovedContacts(
      MOORLAND_PRISONER.number,
      contactId = 123,
      prisonerContactId = 456,
    )
    personalRelationshipsApi().stubPrisonerContactRelationships(MOORLAND_PRISONER.number, 2L)
    locationsInsidePrisonApi().stubGetLocationById(moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")))
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(
      MOORLAND,
      listOf(
        moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")),
      ),
    )
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `create a visitor - should add an official visitor to a visit`() {
    val contactId = 2L

    val visitRequest = syncCreateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    )

    val visitResponse = webTestClient.syncCreateOfficialVisit(visitRequest)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncOfficialVisit>()
      .returnResult().responseBody!!

    val savedOfficialVisitId = visitResponse.officialVisitId

    stubEvents.reset()

    // Add a visitor
    val request = syncCreateOfficialVisitorRequest(
      offenderVisitVisitorId = 1L,
      contactId,
    )

    val response = webTestClient.syncCreateOfficialVisitor(savedOfficialVisitId, request)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncOfficialVisitor>()
      .returnResult().responseBody!!

    assertThat(response.officialVisitorId).isGreaterThan(0)
    assertThat(response.firstName).isEqualTo(request.firstName)
    assertThat(response.lastName).isEqualTo(request.lastName)
    assertThat(response.contactId).isEqualTo(request.personId)
    assertThat(response.relationshipType).isEqualTo(request.relationshipTypeCode)
    assertThat(response.relationshipCode).isEqualTo(request.relationshipToPrisoner)
    assertThat(response.leadVisitor).isEqualTo(request.groupLeaderFlag)
    assertThat(response.assistedVisit).isEqualTo(request.assistedVisitFlag)
    assertThat(response.visitorNotes).isEqualTo(request.commentText)

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_CREATED,
      additionalInfo = VisitorInfo(
        officialVisitId = savedOfficialVisitId,
        officialVisitorId = response.officialVisitorId,
        source = Source.NOMIS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `remove a visitor - should remove a visitor from a visit`() {
    // Create a visit with a single visitor
    val savedVisit = testAPIClient.createOfficialVisit(officialVisitRequest, MOORLAND_PRISON_USER)
    stubEvents.reset()

    assertThat(savedVisit.officialVisitorIds).hasSize(1)

    webTestClient.syncDeleteVisitor(
      officialVisitId = savedVisit.officialVisitId,
      officialVisitorId = savedVisit.officialVisitorIds.first(),
    ).expectStatus().isNoContent

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISITOR_DELETED,
      additionalInfo = VisitorInfo(
        officialVisitId = savedVisit.officialVisitId,
        officialVisitorId = savedVisit.officialVisitorIds.first(),
        source = Source.NOMIS,
        username = "NOMIS",
        prisonId = MOORLAND,
      ),
    )

    // Get it again and check there are no visitors left
    val changedVisit = webTestClient.syncGetVisit(savedVisit.officialVisitId)
    assertThat(changedVisit.visitors).hasSize(0)
  }

  @Test
  fun `remove a visitor - should silently succeed when the visit does not exist`() {
    webTestClient.syncDeleteVisitor(999L, 1L).expectStatus().isNoContent
    stubEvents.assertHasNoEvents(OutboundEvent.VISITOR_DELETED)
  }

  @Test
  fun `remove a visitor - should silently succeed when the visitor does not exist on this visit`() {
    // Create a visit with no visitors
    val visitRequest = syncCreateOfficialVisitRequest(
      offenderVisitId = 1L,
      prisonVisitSlotId = 1L,
      dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    )

    val visitResponse = webTestClient.syncCreateOfficialVisit(visitRequest)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncOfficialVisit>()
      .returnResult().responseBody!!

    assertThat(visitResponse.offenderVisitId).isGreaterThan(0)
    assertThat(visitResponse.visitors).hasSize(0)
    stubEvents.reset()

    webTestClient.syncDeleteVisitor(visitResponse.officialVisitId, 99L).expectStatus().isNoContent

    stubEvents.assertHasNoEvents(OutboundEvent.VISITOR_DELETED)
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

  private fun syncCreateOfficialVisitorRequest(
    offenderVisitVisitorId: Long,
    contactId: Long,
  ) = SyncCreateOfficialVisitorRequest(
    offenderVisitVisitorId = offenderVisitVisitorId,
    personId = contactId,
    firstName = "First",
    lastName = "Last",
    dateOfBirth = LocalDate.of(1980, 1, 1),
    relationshipTypeCode = RelationshipType.OFFICIAL,
    relationshipToPrisoner = "POL",
    groupLeaderFlag = true,
    assistedVisitFlag = true,
    commentText = "comment text",
    createUsername = MOORLAND_PRISON_USER.username,
    createDateTime = LocalDateTime.now().minusMinutes(10),
  )

  private fun WebTestClient.syncCreateOfficialVisitor(
    officialVisitId: Long,
    request: SyncCreateOfficialVisitorRequest,
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
  ) = this
    .post()
    .uri("/sync/official-visit/{officialVisitId}/visitor", officialVisitId)
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_MIGRATION")))
    .exchange()

  private fun WebTestClient.syncCreateOfficialVisit(request: SyncCreateOfficialVisitRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .post()
    .uri("/sync/official-visit")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_MIGRATION")))
    .exchange()

  private fun WebTestClient.syncDeleteVisitor(officialVisitId: Long, officialVisitorId: Long, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .delete()
    .uri("/sync/official-visit/{officialVisitId}/visitor/{officialVisitorId}", officialVisitId, officialVisitorId)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_MIGRATION")))
    .exchange()

  private fun WebTestClient.syncGetVisit(officialVisitId: Long) = this.get()
    .uri("/sync/official-visit/id/{officialVisitId}", officialVisitId)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<SyncOfficialVisit>()
    .returnResult().responseBody!!
}
