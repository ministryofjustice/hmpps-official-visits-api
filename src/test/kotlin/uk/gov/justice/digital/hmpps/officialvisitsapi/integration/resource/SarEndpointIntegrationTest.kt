package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.TestConfiguration
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCompletionRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitorAttendance
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar.SubjectAccessResponseData
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import java.time.LocalDate
import java.util.UUID

/**
 * These tests check that the SAR endpoint returns the expected data.
 * They do not check that the SAR template is rendered or that it contains the correct information.
 */

@Import(TestConfiguration::class)
class SarEndpointIntegrationTest : IntegrationTestBase() {

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

  private val visitRequest = createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor))

  @BeforeEach
  fun `set up official visit and change history data`() {
    clearAllVisitData()
    stubEvents.reset()

    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = MOORLAND_PRISONER.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = MOORLAND_PRISONER.number,
          type = "O",
          contactId = CONTACT_MOORLAND_PRISONER.contactId,
          prisonerContactId = CONTACT_MOORLAND_PRISONER.prisonerContactId,
        ),
      ),
    )
    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER)
    personalRelationshipsApi().stubReferenceGroup()
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(prisonCode = MOORLAND, locations = listOf(location))
    locationsInsidePrisonApi().stubGetLocationById(location)
  }

  @Test
  fun `SAR endpoint should return expected data`() {
    auditedEventRepository.findAll() hasSize 0

    // Create the visit
    val scheduledVisit = testAPIClient.createOfficialVisit(visitRequest, MOORLAND_PRISON_USER)
      .let { response -> testAPIClient.getOfficialVisitBy(response.officialVisitId, MOORLAND_PRISON_USER) }

    // Check it exists
    with(scheduledVisit) {
      visitStatus isEqualTo VisitStatusType.SCHEDULED
      completionCode isEqualTo null
      updatedBy isEqualTo null
      updatedTime isEqualTo null
    }

    // Complete the visit to add audited event rows
    webTestClient.complete(
      officialVisitId = scheduledVisit.officialVisitId,
      request = OfficialVisitCompletionRequest(
        completionReason = VisitCompletionType.PRISONER_REFUSED,
        completionNotes = "Prisoner refused to attend",
        prisonerAttendance = AttendanceType.ABSENT,
        prisonerSearchType = SearchLevelType.FULL,
        visitorAttendance = listOf(
          OfficialVisitorAttendance(
            scheduledVisit.officialVisitors!!.single().officialVisitorId,
            AttendanceType.ATTENDED,
          ),
        ),
      ),
    )

    val completedVisit = testAPIClient.getOfficialVisitBy(scheduledVisit.officialVisitId, MOORLAND_PRISON_USER)

    with(completedVisit) {
      visitStatus isEqualTo VisitStatusType.COMPLETED
      completionCode isEqualTo VisitCompletionType.PRISONER_REFUSED
      completionNotes isEqualTo "Prisoner refused to attend"
      updatedBy isEqualTo MOORLAND_PRISON_USER.username
      updatedTime isCloseTo now()
    }

    // Get the SAR content for the period yesterday to today + 8 days - test will create this visit on the next Monday.
    val response = webTestClient.getSarContent(MOORLAND_PRISONER.number, today().minusDays(1), today().plusDays(8))

    with(response.content.officialVisits.first()) {
      visitType isEqualTo VisitType.IN_PERSON
      visitStatus isEqualTo VisitStatusType.COMPLETED
      prisonerAttendance isEqualTo AttendanceType.ABSENT
      staffNotes isEqualTo "private notes"
      prisonerNotes isEqualTo "public notes"

      with(visitors.first()) {
        relationshipType isEqualTo RelationshipType.OFFICIAL
        relationshipCode isEqualTo "POM"
        visitorAttendance isEqualTo AttendanceType.ATTENDED
      }
    }
  }

  private fun WebTestClient.getSarContent(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = get()
    .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<SubjectAccessRequestContent>()
    .returnResult().responseBody!!

  @Test
  fun `SAR API should return a 204 no content when no data`() {
    webTestClient.noSarContent("B1234BB", LocalDate.now().minusDays(1), LocalDate.now())
  }

  private fun WebTestClient.noSarContent(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = get()
    .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isNoContent

  private fun WebTestClient.complete(officialVisitId: Long, request: OfficialVisitCompletionRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = this
    .post()
    .uri("/official-visit/prison/${prisonUser.caseloads.first()}/id/$officialVisitId/complete")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = prisonUser.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
}

data class SubjectAccessRequestContent(val content: SubjectAccessResponseData)
