package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isBool
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitNotification
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.NotificationType
import java.util.UUID

class NotificationRetrievalIntegrationTest : IntegrationTestBase() {

  @MockitoBean
  private lateinit var metricsService: MetricsService

  private val location = moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))

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

  private val nextMondayAt9 = createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor))

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()

    locationsInsidePrisonApi().stubGetLocationById(location)
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(MOORLAND, locations = listOf(location))

    // Stub a known contact
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = officialVisitor.contactId!!,
          prisonerContactId = officialVisitor.prisonerContactId!!,
        ),
      ),
    )

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
  }

  @Test
  fun `should return notifications for an official visit id`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    testAPIClient.sendNotification(
      officialVisitId = scheduledVisit.officialVisitId,
      notificationType = NotificationType.CREATE,
      emailAddresses = listOf("email@address.com", "email2@address.com"),
    )

    val notifications = webTestClient.getNotificationsByOfficialVisitId(
      scheduledVisit.officialVisitId,
      sort = "sortDirection=ASC",
    )

    notifications.size isEqualTo 2
    notifications.map { it.officialVisitId }.distinct() containsExactly listOf(scheduledVisit.officialVisitId)
    notifications.map { it.emailAddress } containsExactlyInAnyOrder listOf("email@address.com", "email2@address.com")
    notifications.map { it.reason }.distinct() containsExactly listOf("OFFICIAL_VISIT_CREATED")
  }

  @Test
  fun `should return notifications for an official visit id with sort ascending or descending`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    val older = testAPIClient.sendNotification(
      officialVisitId = scheduledVisit.officialVisitId,
      notificationType = NotificationType.CREATE,
      emailAddresses = listOf("email@address.com", "email2@address.com"),
    )

    val newer = testAPIClient.sendNotification(
      officialVisitId = scheduledVisit.officialVisitId,
      notificationType = NotificationType.AMEND,
      emailAddresses = listOf("email@address.com"),
    )

    val resultDesc = webTestClient.getNotificationsByOfficialVisitId(
      scheduledVisit.officialVisitId,
      sort = "sortDirection=DESC",
    ).map { it.notificationId }

    assertThat(resultDesc).containsExactly(
      newer.recipients.first().notificationId,
      older.recipients.last().notificationId,
      older.recipients.first().notificationId,
    )

    val resultAsc = webTestClient.getNotificationsByOfficialVisitId(
      scheduledVisit.officialVisitId,
      sort = "sortDirection=ASC",
    ).map { it.notificationId }

    assertThat(resultAsc).containsExactly(
      older.recipients.first().notificationId,
      older.recipients.last().notificationId,
      newer.recipients.first().notificationId,
    )
  }

  @Test
  fun `should return empty notifications list when none exist for official visit id`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    val notifications = webTestClient.getNotificationsByOfficialVisitId(
      scheduledVisit.officialVisitId,
      sort = "sortDirection=ASC",
    )

    notifications.isEmpty() isBool true
  }

  @Test
  fun `should return 404 when no visit exists for id`() {
    webTestClient.getNotificationsByOfficialVisitIdNotFound(999L)
  }

  private fun WebTestClient.getNotificationsByOfficialVisitId(
    officialVisitId: Long,
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
    sort: String,
  ) = this
    .get()
    .uri("/official-visit/id/$officialVisitId/notifications?$sort")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<List<OfficialVisitNotification>>()
    .returnResult().responseBody!!

  private fun WebTestClient.getNotificationsByOfficialVisitIdNotFound(
    officialVisitId: Long,
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
  ) = this
    .get()
    .uri("/official-visit/id/$officialVisitId/notifications")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isNotFound
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody().jsonPath("$.userMessage").isEqualTo("Official visit with id $officialVisitId not found")
}
