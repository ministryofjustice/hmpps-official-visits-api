package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationSearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.SentNotification
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.NotificationSearchInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.NotificationType
import java.time.LocalDate
import java.util.UUID

class NotificationSearchIntegrationTest : IntegrationTestBase() {

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
  fun `should search sent notifications with pagination`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    testAPIClient.sendNotification(
      officialVisitId = scheduledVisit.officialVisitId,
      notificationType = NotificationType.CREATE,
      emailAddresses = listOf("email@address.com"),
    )

    val result = webTestClient.searchSentNotifications(
      prisonCode = MOORLAND,
      request = NotificationSearchRequest(fromDate = null, toDate = null),
      page = 0,
      size = 10,
    )

    result.page.totalElements isEqualTo 1L
    result.content.size isEqualTo 1
    result.page.totalPages isEqualTo 1L
    result.page.number isEqualTo 0L
    result.page.size isEqualTo 10L

    with(result.content[0]) {
      officialVisitId isEqualTo scheduledVisit.officialVisitId
      emailAddress isEqualTo "email@address.com"
      emailStatus isEqualTo "PENDING"
      notificationType isEqualTo "CREATE"
      notificationTypeDescription isEqualTo "Visit Created"
      prisonerNumber isEqualTo MOORLAND_PRISONER.number
      firstName isEqualTo MOORLAND_PRISONER.firstName
      lastName isEqualTo MOORLAND_PRISONER.lastName
    }

    verify(metricsService).send(
      eventType = eq(MetricsEvents.NOTIFICATION_SEARCH),
      info = any<NotificationSearchInfo>(),
    )
  }

  @Test
  fun `should search notifications with a date filter`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    testAPIClient.sendNotification(
      officialVisitId = scheduledVisit.officialVisitId,
      notificationType = NotificationType.CREATE,
      emailAddresses = listOf("email@address.com"),
    )

    val resultWithMatch = webTestClient.searchSentNotifications(
      prisonCode = MOORLAND,
      request = NotificationSearchRequest(
        fromDate = LocalDate.now().minusDays(1),
        toDate = LocalDate.now().plusDays(1),
      ),
      page = 0,
      size = 10,
    )

    resultWithMatch.page.totalElements isEqualTo 1L
    resultWithMatch.content.size isEqualTo 1

    val resultWithoutMatch = webTestClient.searchSentNotifications(
      prisonCode = MOORLAND,
      request = NotificationSearchRequest(
        fromDate = LocalDate.now().plusDays(30),
        toDate = LocalDate.now().plusDays(60),
      ),
      page = 0,
      size = 10,
    )

    resultWithoutMatch.page.totalElements isEqualTo 0L
    resultWithoutMatch.content.isEmpty()
  }

  @Test
  fun `should find notifications for the day when from and to dates are the same`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    testAPIClient.sendNotification(
      officialVisitId = scheduledVisit.officialVisitId,
      notificationType = NotificationType.CREATE,
      emailAddresses = listOf("email@address.com"),
    )

    val today = LocalDate.now()
    val result = webTestClient.searchSentNotifications(
      prisonCode = MOORLAND,
      request = NotificationSearchRequest(fromDate = today, toDate = today),
      page = 0,
      size = 10,
    )

    result.page.totalElements isEqualTo 1L
    result.content.size isEqualTo 1
  }

  @Test
  fun `should return no notifications for a different prison code`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    testAPIClient.sendNotification(
      officialVisitId = scheduledVisit.officialVisitId,
      notificationType = NotificationType.CREATE,
      emailAddresses = listOf("email@address.com"),
    )

    val result = webTestClient.searchSentNotifications(
      prisonCode = PENTONVILLE,
      request = NotificationSearchRequest(fromDate = null, toDate = null),
      page = 0,
      size = 10,
    )

    result.page.totalElements isEqualTo 0L
    result.content.isEmpty()
  }

  private fun WebTestClient.searchSentNotifications(
    prisonCode: String,
    request: NotificationSearchRequest,
    page: Int = 0,
    size: Int = 10,
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
  ) = this
    .post()
    .uri("/notification/prison/$prisonCode/sent-emails?page=$page&size=$size")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<NotificationSearchResponse>()
    .returnResult().responseBody!!
}

private data class NotificationSearchResponse(
  val content: List<SentNotification>,
  val page: PageMetadata,
)

private data class PageMetadata(
  val size: Long,
  val number: Long,
  val totalElements: Long,
  val totalPages: Long,
)
