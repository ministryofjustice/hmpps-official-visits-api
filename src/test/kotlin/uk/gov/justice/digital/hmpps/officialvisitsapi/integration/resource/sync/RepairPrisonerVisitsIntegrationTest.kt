package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.RepairPrisonerVisitsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.RepairPrisonerVisitsResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class RepairPrisonerVisitsIntegrationTest : IntegrationTestBase() {

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
    prisonVisitSlotId = 1, // Loaded in base data within the test context
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
  fun `should replace the visits for a prisoner with freshly migrated versions`() {
    // Create a visit
    val savedOfficialVisitId = (testAPIClient.createOfficialVisit(officialVisitRequest, MOORLAND_PRISON_USER)).officialVisitId
    stubEvents.reset()

    // Build a list of visits to replace the original one - the visit slots are loaded in the test context
    // Slot 1 = 9-10am Monday
    // Slot 2 = 10-11am Monday
    // Slot 3 = 11-12am Monday
    val request = RepairPrisonerVisitsRequest(
      visits = getMigrateVisitsRequest(slot1Id = 2, slot2Id = 3),
    )

    // Call the repair endpoint
    val repairResult = webTestClient.repairPrisonerVisits(MOORLAND_PRISONER.number, request, MOORLAND_PRISON_USER)

    // Get the count of visits created from the response list
    assertThat(repairResult.prisonerNumber).isEqualTo(MOORLAND_PRISONER.number)
    assertThat(repairResult.visits).hasSize(2)

    // Compare the IDs returned - DPS and NOMIS - first visit
    assertThat(repairResult.visits.first().visit.dpsId).isGreaterThan(1L)
    assertThat(repairResult.visits.first().visit.nomisId).isEqualTo(1)

    // Compare the IDs returned - DPS and NOMIS - second visit
    assertThat(repairResult.visits.last().visit.dpsId).isGreaterThan(1L)
    assertThat(repairResult.visits.last().visit.nomisId).isEqualTo(2)

    // Check the original visit is no longer present
    webTestClient.getOfficialVisitByPrisonAndId(MOORLAND, savedOfficialVisitId)
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Official visit with id $savedOfficialVisitId and prison code $MOORLAND not found")

    // Get the visit details which replaced it and validate they match on key details that were requested in the repair
    val replacingVisits = webTestClient.getAllOfficialVisitForPrisoner(MOORLAND_PRISONER.number, visitDateInTheFuture)

    assertThat(replacingVisits).hasSize(2)
    assertThat(replacingVisits).extracting("offenderVisitId").containsAll(listOf(1L, 2L))
    assertThat(replacingVisits).extracting("visitOrderNumber").containsAll(listOf(333L, 333L))
    assertThat(replacingVisits).extracting("statusCode").containsAll(listOf(VisitStatusType.SCHEDULED, VisitStatusType.COMPLETED))

    // No sync events should be generated as part of this replacement
    stubEvents.assertHasNoEvents(OutboundEvent.VISIT_CREATED)
    stubEvents.assertHasNoEvents(OutboundEvent.VISIT_DELETED)
  }

  private fun WebTestClient.repairPrisonerVisits(
    prisonerNumber: String,
    request: RepairPrisonerVisitsRequest,
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
  ) = this.post()
    .uri("/repair/prisoner-visits/$prisonerNumber", prisonerNumber)
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<RepairPrisonerVisitsResponse>()
    .returnResult().responseBody!!

  private fun WebTestClient.getOfficialVisitByPrisonAndId(prisonCode: String, officialVisitId: Long) = this
    .get()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()

  private fun WebTestClient.getAllOfficialVisitForPrisoner(prisonerNumber: String, toDate: LocalDate?) = this
    .get()
    .uri("/reconcile/prisoner/$prisonerNumber?currentTermOnly=true&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList<SyncOfficialVisit>()
    .returnResult().responseBody!!

  private fun getMigrateVisitsRequest(slot1Id: Long, slot2Id: Long) = listOf(
    migrateVisitRequest(
      offenderVisitId = 1,
      visitDate = visitDateInTheFuture,
      visitStatus = VisitStatusType.SCHEDULED,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      prisonVisitSlotId = slot1Id,
    ),
    migrateVisitRequest(
      offenderVisitId = 2,
      visitDate = visitDateInTheFuture,
      visitStatus = VisitStatusType.COMPLETED,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(12, 0),
      prisonVisitSlotId = slot2Id,
    ),
  )

  private fun migrateVisitRequest(
    offenderVisitId: Long,
    visitDate: LocalDate,
    visitStatus: VisitStatusType,
    startTime: LocalTime,
    endTime: LocalTime,
    prisonVisitSlotId: Long,
  ) = MigrateVisitRequest(
    offenderVisitId = offenderVisitId,
    prisonVisitSlotId = prisonVisitSlotId,
    prisonCode = MOORLAND,
    offenderBookId = MOORLAND_PRISONER.bookingId,
    prisonerNumber = MOORLAND_PRISONER.number,
    currentTerm = true,
    visitDate = visitDate,
    startTime = startTime,
    endTime = endTime,
    dpsLocationId = UUID.randomUUID(),
    visitStatusCode = visitStatus,
    visitTypeCode = VisitType.UNKNOWN,
    commentText = "A comment",
    searchTypeCode = SearchLevelType.RUB_A,
    visitCompletionCode = VisitCompletionType.NORMAL,
    visitorConcernText = "Concern text",
    overrideBanStaffUsername = "XXX",
    visitOrderNumber = 333,
    createDateTime = LocalDateTime.now(),
    createUsername = MOORLAND_PRISON_USER.username,
    visitors = listOf(
      MigrateVisitor(
        offenderVisitVisitorId = offenderVisitId, // Make it uniques
        personId = CONTACT_MOORLAND_PRISONER.contactId,
        firstName = CONTACT_MOORLAND_PRISONER.firstName,
        lastName = CONTACT_MOORLAND_PRISONER.lastName,
        relationshipTypeCode = RelationshipType.OFFICIAL,
        relationshipToPrisoner = "POL",
        groupLeaderFlag = true,
        assistedVisitFlag = false,
        commentText = "comment1",
        attendanceCode = AttendanceType.ABSENT,
        createDateTime = LocalDateTime.now(),
        createUsername = MOORLAND_PRISON_USER.username,
      ),
    ),
  )
}
