package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Moorland
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.location
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.nonAssociation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerNonAssociations
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NonAssociationCheckRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NonAssociationVisitResponse
import kotlin.properties.Delegates

class NonAssociationCheckIntegrationTest : IntegrationTestBase() {
  private val nonAssociate = Prisoner(MOORLAND, "A1232DD", 2, "Steve", "Smith")

  private val nonAssociateVisit = createOfficialVisitRequest(
    Moorland.MONDAY_9_TO_10_VISIT_SLOT,
    listOf(Moorland.VISITOR),
    prisonerNumber = nonAssociate.number,
  )

  private var nonAssociateVisitId by Delegates.notNull<Long>()

  private val request = NonAssociationCheckRequest(
    prisonerNumber = MOORLAND_PRISONER.number,
    visitDate = nonAssociateVisit.visitDate,
    startTime = nonAssociateVisit.startTime,
    endTime = nonAssociateVisit.endTime,
  )

  @BeforeEach
  @Transactional
  fun beforeEach() {
    clearAllVisitData()

    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(
      prisonCode = MOORLAND,
      locations = listOf(
        location(
          prisonCode = MOORLAND,
          locationKeySuffix = "1-1",
          localName = "Visit place",
          id = moorlandLocation.id,
        ),
      ),
    )

    prisonerSearchApi().stubGetPrisoner(nonAssociate)
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = nonAssociate.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = nonAssociate.number,
          type = "O",
          contactId = Moorland.VISITOR.contactId!!,
          prisonerContactId = Moorland.VISITOR.prisonerContactId!!,
        ),
      ),
    )

    nonAssociateVisitId = testAPIClient.createOfficialVisit(nonAssociateVisit).officialVisitId
  }

  @Test
  fun `should return no visits when the prisoner has no non-associations`() {
    nonAssociationsApi().stubGetPrisonerNonAssociations(
      prisonerNumber = MOORLAND_PRISONER.number,
      nonAssociations = prisonerNonAssociations(nonAssociations = emptyList()),
    )

    webTestClient.check(request) hasSize 0
  }

  @Test
  fun `should return no visits when the prisoner is unknown to the non-associations service`() {
    nonAssociationsApi().stubGetPrisonerNonAssociationsNotFound(MOORLAND_PRISONER.number)

    webTestClient.check(request) hasSize 0
  }

  @Test
  fun `should return no visits when the non-associate has no visit on the requested date`() {
    nonAssociationsApi().stubGetPrisonerNonAssociations(
      prisonerNumber = MOORLAND_PRISONER.number,
      nonAssociations = prisonerNonAssociations(nonAssociations = listOf(nonAssociation(nonAssociate))),
    )

    webTestClient.check(request.copy(visitDate = request.visitDate.plusDays(1))) hasSize 0
  }

  @Test
  fun `should return no visits when the non-associate's visit is at a different prison`() {
    nonAssociationsApi().stubGetPrisonerNonAssociations(
      prisonerNumber = MOORLAND_PRISONER.number,
      nonAssociations = prisonerNonAssociations(nonAssociations = listOf(nonAssociation(nonAssociate))),
    )

    webTestClient.check(request, prisonCode = "PVI") hasSize 0
  }

  @Test
  fun `should return the non-associate's visit and details when they have a visit on the requested date`() {
    nonAssociationsApi().stubGetPrisonerNonAssociations(
      prisonerNumber = MOORLAND_PRISONER.number,
      nonAssociations = prisonerNonAssociations(nonAssociations = listOf(nonAssociation(nonAssociate))),
    )

    with(webTestClient.check(request).single()) {
      prisonCode isEqualTo MOORLAND
      officialVisitId isEqualTo nonAssociateVisitId
      visitDate isEqualTo nonAssociateVisit.visitDate
      startTime isEqualTo nonAssociateVisit.startTime
      endTime isEqualTo nonAssociateVisit.endTime
      prisonerNumber isEqualTo nonAssociate.number
      dpsLocationId isEqualTo moorlandLocation.id
      locationDescription isEqualTo "Visit place"
      firstName isEqualTo "Steve"
      lastName isEqualTo "Smith"
    }
  }

  @Test
  fun `should return a visit for each non-associate with a visit on the requested date`() {
    val otherNonAssociate = Prisoner(MOORLAND, "A1233EE", 3, "Alan", "Jones")

    prisonerSearchApi().stubGetPrisoner(otherNonAssociate)
    personalRelationshipsApi().stubAllContacts(
      prisonerNumber = otherNonAssociate.number,
      prisonerContacts = listOf(
        prisonerContact(
          prisonerNumber = otherNonAssociate.number,
          type = "O",
          contactId = Moorland.VISITOR.contactId!!,
          prisonerContactId = Moorland.VISITOR.prisonerContactId!!,
        ),
      ),
    )

    val otherNonAssociateVisitId = testAPIClient.createOfficialVisit(
      createOfficialVisitRequest(
        Moorland.MONDAY_9_TO_10_VISIT_SLOT,
        listOf(Moorland.VISITOR),
        prisonerNumber = otherNonAssociate.number,
      ),
    ).officialVisitId

    nonAssociationsApi().stubGetPrisonerNonAssociations(
      prisonerNumber = MOORLAND_PRISONER.number,
      nonAssociations = prisonerNonAssociations(
        nonAssociations = listOf(
          nonAssociation(nonAssociate, id = 1),
          nonAssociation(otherNonAssociate, id = 2),
        ),
      ),
    )

    val response = webTestClient.check(request)

    response.map { it.officialVisitId } containsExactly listOf(nonAssociateVisitId, otherNonAssociateVisitId)
    response.map { it.prisonerNumber } containsExactly listOf(nonAssociate.number, otherNonAssociate.number)
    response.map { it.lastName } containsExactly listOf("Smith", "Jones")
  }

  @Test
  fun `should return the non-associate's visit when no times are supplied`() {
    nonAssociationsApi().stubGetPrisonerNonAssociations(
      prisonerNumber = MOORLAND_PRISONER.number,
      nonAssociations = prisonerNonAssociations(nonAssociations = listOf(nonAssociation(nonAssociate))),
    )

    webTestClient.check(request.copy(startTime = null, endTime = null)) hasSize 1
  }

  @Test
  fun `should return no visits when the supplied times do not overlap the non-associate's visit`() {
    nonAssociationsApi().stubGetPrisonerNonAssociations(
      prisonerNumber = MOORLAND_PRISONER.number,
      nonAssociations = prisonerNonAssociations(nonAssociations = listOf(nonAssociation(nonAssociate))),
    )

    webTestClient.check(
      request.copy(startTime = nonAssociateVisit.endTime, endTime = nonAssociateVisit.endTime.plusHours(1)),
    ) hasSize 0
  }

  @Test
  fun `should reject a start time without an end time`() {
    webTestClient.post()
      .uri("/official-visit/non-association-check/prison/$MOORLAND")
      .bodyValue(request.copy(endTime = null))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.userMessage").isEqualTo("The start and end times must be supplied together")
  }

  @Test
  fun `should be accessible to the read write role`() {
    nonAssociationsApi().stubGetPrisonerNonAssociations(
      prisonerNumber = MOORLAND_PRISONER.number,
      nonAssociations = prisonerNonAssociations(nonAssociations = listOf(nonAssociation(nonAssociate))),
    )

    val response = webTestClient.check(request, role = "ROLE_OFFICIAL_VISITS__RW")

    response.map { it.prisonerNumber } containsExactly listOf(nonAssociate.number)
  }

  @Test
  fun `should be forbidden without an appropriate role`() {
    webTestClient.post()
      .uri("/official-visit/non-association-check/prison/$MOORLAND")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `should be unauthorised without a token`() {
    webTestClient.post()
      .uri("/official-visit/non-association-check/prison/$MOORLAND")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  private fun WebTestClient.check(
    request: NonAssociationCheckRequest,
    prisonCode: String = MOORLAND,
    role: String = "ROLE_OFFICIAL_VISITS_ADMIN",
  ) = this
    .post()
    .uri("/official-visit/non-association-check/prison/$prisonCode")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(role)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList<NonAssociationVisitResponse>()
    .returnResult().responseBody!!
}
