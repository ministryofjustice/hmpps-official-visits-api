package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.CONTACT_MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.VisitorEquipment
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails

class OfficialVisitRetrievalIntegrationTest : IntegrationTestBase() {

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

  private val nextMondayAt9 = createOfficialVisitRequest(Moorland.MONDAY_9_TO_10_VISIT_SLOT, listOf(officialVisitor))

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()

    // Stub contacts
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
    personalRelationshipsApi().stubForContactById(CONTACT_MOORLAND_PRISONER, "contact@email.address")
    personalRelationshipsApi().stubReferenceGroup()

    // Stub locations
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(MOORLAND, listOf(moorlandLocation))
    locationsInsidePrisonApi().stubGetLocationById(moorlandLocation)
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should get an official visit by prison code and ID`() {
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
      visitDate isEqualTo nextMondayAt9.visitDate
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

  @Test
  fun `should error when the ID does not exist at the prison provided`() {
    webTestClient.getOfficialVisitByInvalidPrisonAndId(MOORLAND_PRISONER.prison, 999L)
  }

  @Test
  fun `should get an official visit by ID`() {
    val response = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    val visitDetail = webTestClient.getOfficialVisitById(response.officialVisitId)

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
      visitDate isEqualTo nextMondayAt9.visitDate
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

  @Test
  fun `should error 404 when the visit prison is not in the users caseloads`() {
    val response = testAPIClient.createOfficialVisit(nextMondayAt9, MOORLAND_PRISON_USER)

    stubUser(PENTONVILLE_PRISON_USER)

    webTestClient.getOfficialVisitByIdNotInCaseload(response.officialVisitId)
  }

  @Test
  fun `should error when the ID does not exist`() {
    webTestClient.getOfficialVisitByInvalidId(999L)
  }

  private fun WebTestClient.getOfficialVisitByPrisonAndId(prisonCode: String, officialVisitId: Long) = this
    .get()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<OfficialVisitDetails>()
    .returnResult().responseBody!!

  private fun WebTestClient.getOfficialVisitByInvalidPrisonAndId(prisonCode: String, officialVisitId: Long) = this
    .get()
    .uri("/official-visit/prison/$prisonCode/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN"))).exchange()
    .expectStatus().is4xxClientError
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody().jsonPath("$.userMessage").isEqualTo("Official visit with id $officialVisitId and prison code $prisonCode not found")

  private fun WebTestClient.getOfficialVisitById(officialVisitId: Long) = this
    .get()
    .uri("/official-visit/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<OfficialVisitDetails>()
    .returnResult().responseBody!!

  private fun WebTestClient.getOfficialVisitByInvalidId(officialVisitId: Long) = this
    .get()
    .uri("/official-visit/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN"))).exchange()
    .expectStatus().is4xxClientError
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody().jsonPath("$.userMessage").isEqualTo("Official visit with id $officialVisitId was not found")

  private fun WebTestClient.getOfficialVisitByIdNotInCaseload(officialVisitId: Long) = this
    .get()
    .uri("/official-visit/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = PENTONVILLE_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN"))).exchange()
    .expectStatus().isNotFound
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody().jsonPath("$.userMessage").isEqualTo("The visit was not found or is restricted by caseload")
}
