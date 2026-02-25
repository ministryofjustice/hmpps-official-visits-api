package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class GetOfficialVisitByIdIntegrationTest : IntegrationTestBase() {

  private val officialVisitor = OfficialVisitor(
    visitorTypeCode = VisitorType.CONTACT,
    relationshipCode = "POM",
    contactId = CONTACT_MOORLAND_PRISONER.contactId,
    prisonerContactId = CONTACT_MOORLAND_PRISONER.prisonerContactId,
    leadVisitor = true,
    assistedVisit = true,
    visitorEquipment = VisitorEquipment("Laptop"),
    assistedNotes = "Wheelchair access needed",
  )

  private final val visitDateInTheFuture = today().next(DayOfWeek.MONDAY)

  private val nextMondayAt9 = CreateOfficialVisitRequest(
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
  fun setupTest() {
    clearAllVisitData()
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should error when the ID does not exist`() {
    webTestClient.getOfficialVisitByInvalidPrisonAndId(MOORLAND_PRISONER.prison, 999L)
  }

  @Test
  fun `should get an official visit by prison code and ID`() {
    personalRelationshipsApi().stubAllApprovedContact(CONTACT_MOORLAND_PRISONER)
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER, "contact@email.address")
    personalRelationshipsApi().stubReferenceGroup()
    locationsInsidePrisonApi().stubGetLocationById(moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")))

    val response = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)
    val visitDetail = webTestClient.getOfficialVisitByPrisonAndId(MOORLAND_PRISONER.prison, response.officialVisitId)

    with(visitDetail) {
      officialVisitId isEqualTo response.officialVisitId
      prisonCode isEqualTo MOORLAND_PRISONER.prison
      prisonerVisited?.prisonerNumber isEqualTo nextMondayAt9.prisonerNumber
      dpsLocationId isEqualTo nextMondayAt9.dpsLocationId
      locationDescription isEqualTo moorlandLocation.localName
      visitTypeCode isEqualTo nextMondayAt9.visitTypeCode
      visitStatus isEqualTo VisitStatusType.SCHEDULED
      staffNotes isEqualTo nextMondayAt9.staffNotes
      prisonerNotes isEqualTo nextMondayAt9.prisonerNotes
      visitDate isEqualTo visitDateInTheFuture
      startTime isEqualTo nextMondayAt9.startTime
      endTime isEqualTo nextMondayAt9.endTime
    }

    with(visitDetail.officialVisitors!!.single()) {
      firstName isEqualTo CONTACT_MOORLAND_PRISONER.firstName
      lastName isEqualTo CONTACT_MOORLAND_PRISONER.lastName
      visitorEquipment!!.description isEqualTo "Laptop"
      assistanceNotes isEqualTo "Wheelchair access needed"
      phoneNumber isEqualTo CONTACT_MOORLAND_PRISONER.phoneNumber
      emailAddress isEqualTo "contact@email.address"
    }
  }

  private fun WebTestClient.getOfficialVisitByInvalidPrisonAndId(prisonCode: String, officialVisitId: Long) = this
    .get()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN"))).exchange()
    .expectStatus().is4xxClientError
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody().jsonPath("$.userMessage").isEqualTo("Official visit with id $officialVisitId and prison code $prisonCode not found")

  private fun WebTestClient.getOfficialVisitByPrisonAndId(prisonCode: String, officialVisitId: Long) = this
    .get()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(OfficialVisitDetails::class.java)
    .returnResult().responseBody!!
}
