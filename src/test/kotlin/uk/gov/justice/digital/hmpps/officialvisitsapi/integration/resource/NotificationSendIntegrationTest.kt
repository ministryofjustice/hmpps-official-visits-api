package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NotificationRecipient
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.NotificationType
import java.util.UUID

class NotificationSendIntegrationTest : IntegrationTestBase() {

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
  fun `should send a notification for a visit created`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    val response = testAPIClient.sendNotification(
      officialVisitId = scheduledVisit.officialVisitId,
      notificationType = NotificationType.CREATE,
      emailAddresses = listOf("email@address.com"),
    )

    val notification = notificationRepository.findAll().single()

    with(notification) {
      officialVisitId isEqualTo scheduledVisit.officialVisitId
      emailAddress isEqualTo "email@address.com"
      templateId isEqualTo "fake_template_id"
      reason isEqualTo "OFFICIAL_VISIT_CREATED"
      createdTime isCloseTo now()
    }

    with(response) {
      officialVisitId isEqualTo scheduledVisit.officialVisitId
      recipients containsExactly listOf(NotificationRecipient("email@address.com", notification.notificationId))
    }
  }

  @Test
  fun `should send multiple notifications for created visit`() {
    val scheduledVisit = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    val response = testAPIClient.sendNotification(
      officialVisitId = scheduledVisit.officialVisitId,
      notificationType = NotificationType.CREATE,
      emailAddresses = listOf("email1@address.com", "email2s@address.com"),
    )

    val notificationRecipients =
      notificationRepository.findAll().map { NotificationRecipient(it.emailAddress, it.notificationId) }
    notificationRecipients.map { it.emailAddress } containsExactlyInAnyOrder listOf(
      "email1@address.com",
      "email2s@address.com",
    )

    with(response) {
      officialVisitId isEqualTo scheduledVisit.officialVisitId
      recipients containsExactlyInAnyOrder notificationRecipients
    }
  }
}
