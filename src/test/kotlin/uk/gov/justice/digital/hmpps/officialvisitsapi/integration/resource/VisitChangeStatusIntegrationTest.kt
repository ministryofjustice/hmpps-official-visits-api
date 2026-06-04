package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications.NotificationType
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER_ADDED
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitUpdateVisitorsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitChangeStatusResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import java.time.LocalTime
import java.util.UUID

class VisitChangeStatusIntegrationTest : IntegrationTestBase() {

  @MockitoBean
  private lateinit var metricsService: MetricsService

  private val location = moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))

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

  private val nextMondayAt9 = createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor))

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
    stubEvents.reset()

    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(MOORLAND, locations = listOf(location))
    locationsInsidePrisonApi().stubGetLocationById(location)

    // Contacts needed for visit creation and visitor-update audit detail generation
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = CONTACT_MOORLAND_PRISONER.contactId,
          prisonerContactId = CONTACT_MOORLAND_PRISONER.prisonerContactId,
        ),
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = CONTACT_MOORLAND_PRISONER_ADDED.contactId,
          prisonerContactId = CONTACT_MOORLAND_PRISONER_ADDED.prisonerContactId,
        ),
      ),
    )
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER)
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER_ADDED)
    personalRelationshipsApi().stubReferenceGroup()

    // Prisoner search needed for notification email rendering
    prisonerSearchApi().stubSearchPrisonersByPrisonerNumbers(
      idsBeingSearchFor = listOf(MOORLAND_PRISONER.number),
      prisonersToReturn = listOf(
        prisonerSearchPrisoner(
          prisonerNumber = MOORLAND_PRISONER.number,
          prisonCode = MOORLAND,
          firstName = MOORLAND_PRISONER.firstName,
          lastName = MOORLAND_PRISONER.lastName,
          bookingId = MOORLAND_PRISONER.bookingId,
        ),
      ),
    )

    // Prisoner name search needed for cancellation audit detail generation
    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
    stubEvents.reset()
  }

  @Test
  fun `should return false when no notification has been sent for the visit`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    val response = webTestClient.getVisitChangeStatus(scheduledVisit.officialVisitId)

    response.hasChanged isEqualTo true
  }

  @Test
  fun `should return false when no changes have been made since notification was sent`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)
    testAPIClient.sendNotification(scheduledVisit.officialVisitId, NotificationType.CREATE)

    // No visit update — the create audit event predates the notification so is not detected
    val response = webTestClient.getVisitChangeStatus(scheduledVisit.officialVisitId)

    response.hasChanged isEqualTo false
  }

  @Test
  fun `should return true when visit slot is updated after notification sent`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)
    testAPIClient.sendNotification(scheduledVisit.officialVisitId, NotificationType.CREATE)

    webTestClient.updateSlot(
      prisonCode = MOORLAND,
      officialVisitId = scheduledVisit.officialVisitId,
      request = OfficialVisitUpdateSlotRequest(
        prisonVisitSlotId = 1,
        visitDate = nextMondayAt9.visitDate!!.plusMonths(20),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        dpsLocationId = location.id,
        visitTypeCode = VisitType.IN_PERSON,
      ),
    )

    val response = webTestClient.getVisitChangeStatus(scheduledVisit.officialVisitId)

    response.hasChanged isEqualTo true
  }

  @Test
  fun `should return true when visitors are updated after notification sent`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)
    testAPIClient.sendNotification(scheduledVisit.officialVisitId, NotificationType.CREATE)

    // Replace existing visitor with a different one — produces "Visitors added 1; Visitors removed 1"
    webTestClient.updateVisitors(
      prisonCode = MOORLAND,
      officialVisitId = scheduledVisit.officialVisitId,
      request = OfficialVisitUpdateVisitorsRequest(
        officialVisitors = listOf(
          OfficialVisitor(
            visitorTypeCode = VisitorType.CONTACT,
            relationshipCode = "POM",
            contactId = CONTACT_MOORLAND_PRISONER_ADDED.contactId,
            prisonerContactId = CONTACT_MOORLAND_PRISONER_ADDED.prisonerContactId,
            leadVisitor = true,
            assistedVisit = false,
          ),
        ),
      ),
    )

    val response = webTestClient.getVisitChangeStatus(scheduledVisit.officialVisitId)

    response.hasChanged isEqualTo true
  }

  @Test
  fun `should return true when visit is cancelled after notification sent`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)
    testAPIClient.sendNotification(scheduledVisit.officialVisitId, NotificationType.CREATE)

    testAPIClient.cancel(
      officialVisitId = scheduledVisit.officialVisitId,
      request = OfficialVisitCancellationRequest(
        cancellationReason = VisitCompletionType.VISITOR_CANCELLED,
        cancellationNotes = "cancelled during integration test",
      ),
    )

    val response = webTestClient.getVisitChangeStatus(scheduledVisit.officialVisitId)

    response.hasChanged isEqualTo true
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

  private fun WebTestClient.getVisitChangeStatus(officialVisitId: Long) = this
    .get()
    .uri("/notification/$officialVisitId/change-status")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<VisitChangeStatusResponse>()
    .returnResult().responseBody!!
}
