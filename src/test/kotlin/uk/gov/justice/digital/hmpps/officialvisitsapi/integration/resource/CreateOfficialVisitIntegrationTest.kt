package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isNotEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Sql("classpath:integration-test-data/creation/clean-visit-seed-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CreateOfficialVisitIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var officialVisitRepository: OfficialVisitRepository

  @Autowired
  private lateinit var prisonerVisitedRepository: PrisonerVisitedRepository

  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipTypeCode = RelationshipType.OFFICIAL,
    relationshipCode = "POM",
    contactId = 123,
    prisonerContactId = 456,
    firstName = "Bob",
    lastName = "Smith",
    leadVisitor = true,
    assistedVisit = false,
    visitorNotes = "visitor notes",
  )

  private val nextMondayAt9 = CreateOfficialVisitRequest(
    prisonCode = MOORLAND_PRISONER.prison,
    prisonerNumber = MOORLAND_PRISONER.number,
    prisonVisitSlotId = 1,
    visitDate = next(DayOfWeek.MONDAY),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    visitTypeCode = VisitType.IN_PERSON,
    staffNotes = "private notes",
    prisonerNotes = "public notes",
    officialVisitors = listOf(officialVisitor),
  )

  private final val nextFridayAt11 = nextMondayAt9.copy(visitDate = next(DayOfWeek.FRIDAY), startTime = LocalTime.of(11, 0), endTime = LocalTime.of(12, 0), prisonVisitSlotId = 9, dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))

  @Test
  @Transactional
  fun `should create official visit with one social visitor`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)

    val officialVisitResponse = webTestClient.create(nextMondayAt9)
    val persistedOfficialVisit = officialVisitRepository.findById(officialVisitResponse.officialVisitId).get()

    with(persistedOfficialVisit) {
      prisonCode isEqualTo MOORLAND_PRISONER.prison
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      offenderBookId isEqualTo MOORLAND_PRISONER.bookingId
      prisonVisitSlot.prisonVisitSlotId isEqualTo 1
      visitDate isEqualTo next(DayOfWeek.MONDAY)
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
      dpsLocationId isNotEqualTo null
      visitTypeCode isEqualTo VisitType.IN_PERSON
      visitStatusCode isEqualTo VisitStatusType.SCHEDULED
      staffNotes isEqualTo "private notes"
      prisonerNotes isEqualTo "public notes"
      createdBy isEqualTo MOORLAND_PRISON_USER.username
      createdTime isCloseTo now()
    }

    with(persistedOfficialVisit.officialVisitors().single()) {
      visitorTypeCode isEqualTo VisitorType.CONTACT
      relationshipTypeCode isEqualTo RelationshipType.OFFICIAL
      relationshipCode isEqualTo "POM"
      firstName isEqualTo "Bob"
      lastName isEqualTo "Smith"
      leadVisitor isEqualTo true
      assistedVisit isEqualTo false
      visitorNotes isEqualTo "visitor notes"
    }

    with(prisonerVisitedRepository.findAll().single { it.officialVisit == persistedOfficialVisit }) {
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      createdBy isEqualTo MOORLAND_PRISON_USER.username
      createdTime isCloseTo now()
    }
  }

  @Test
  fun `should fail to create official visit when no matching contact found`() {
    webTestClient.badRequest(nextMondayAt9.copy(officialVisitors = listOf(officialVisitor.copy(contactId = 999))), "Visitor with contact ID 999 and prisoner contact ID 456 is not approved for visiting prisoner number ${MOORLAND_PRISONER.number}.")
  }

  @Test
  fun `should fail to create official visit when slot is no longer available`() {
    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)

    webTestClient.create(nextFridayAt11)
    webTestClient.badRequest(nextFridayAt11, "Prison visit slot 9 is no longer available for the requested date and time.")
  }

  @Test
  fun `should fail when no official visitors are provided`() {
    webTestClient.badRequest(nextMondayAt9.copy(officialVisitors = emptyList()), "At least one official visitor must be supplied.")
  }

  @Test
  fun `should fail when unknown prison visit slot id`() {
    webTestClient.badRequest(nextMondayAt9.copy(prisonVisitSlotId = -99), "Prison visit slot with id -99 not found.")
  }

  @Test
  fun `should fail when visit date and time is in the past`() {
    val beforeNow = now().minusMinutes(1)

    webTestClient.badRequest(nextMondayAt9.copy(visitDate = beforeNow.toLocalDate(), startTime = beforeNow.toLocalTime()), "Official visit cannot be scheduled in the past")
  }

  @Test
  fun `should fail when visit start time is the end time`() {
    webTestClient.badRequest(nextMondayAt9.copy(startTime = nextMondayAt9.endTime), "Official visit start time must be before end time")
  }

  @Test
  fun `should fail when prisoner not at prison`() {
    webTestClient.badRequest(nextMondayAt9.copy(prisonCode = WANDSWORTH), "Prisoner ${MOORLAND_PRISONER.number} not found at prison WWI")
  }

  private fun WebTestClient.create(request: CreateOfficialVisitRequest) = this
    .post()
    .uri("/official-visit")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(CreateOfficialVisitResponse::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.badRequest(request: CreateOfficialVisitRequest, errorMessage: String) = this
    .post()
    .uri("/official-visit")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isBadRequest
    .expectBody().jsonPath("$.userMessage").isEqualTo(errorMessage)
}
