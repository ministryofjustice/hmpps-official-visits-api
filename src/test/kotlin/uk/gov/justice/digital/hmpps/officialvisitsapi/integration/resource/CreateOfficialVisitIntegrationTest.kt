package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isNotEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import java.time.LocalTime
import java.util.UUID

class CreateOfficialVisitIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var officialVisitRepository: OfficialVisitRepository

  @Test
  @Transactional
  fun `should create official visit with one social visitor`() {
    val officialVisitResponse = webTestClient.create(
      CreateOfficialVisitRequest(
        prisonCode = PENTONVILLE_PRISONER.prison,
        prisonerNumber = PENTONVILLE_PRISONER.number,
        prisonVisitSlotId = 1,
        visitDate = today().plusDays(1),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        dpsLocationId = UUID.randomUUID(),
        visitTypeCode = "IN_PERSON",
        privateNotes = "private notes",
        publicNotes = "public notes",
        officialVisitors = listOf(
          OfficialVisitor(
            visitorTypeCode = "CONTACT",
            contactTypeCode = "SOCIAL",
            contactId = null,
            prisonerContactId = null,
            firstName = "Bob",
            lastName = "Smith",
            leadVisitor = true,
            assistedVisit = false,
            visitorNotes = "visitor notes",
          ),
        ),
      ),
    )

    val persistedOfficialVisit = officialVisitRepository.findById(officialVisitResponse.officialVisitId).get()

    with(persistedOfficialVisit) {
      prisonCode isEqualTo PENTONVILLE_PRISONER.prison
      prisonerNumber isEqualTo PENTONVILLE_PRISONER.number
      prisonVisitSlot.prisonVisitSlotId isEqualTo 1
      visitDate isEqualTo today().plusDays(1)
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 0)
      dpsLocationId isNotEqualTo null
      visitTypeCode isEqualTo "IN_PERSON"
      visitStatusCode isEqualTo "ACTIVE" // Set on creation
      privateNotes isEqualTo "private notes"
      publicNotes isEqualTo "public notes"
      createdBy isEqualTo PENTONVILLE_PRISON_USER.username
      createdTime isCloseTo now()
    }

    with(persistedOfficialVisit.officialVisitors().single()) {
      visitorTypeCode isEqualTo "CONTACT"
      contactTypeCode isEqualTo "SOCIAL"
      firstName isEqualTo "Bob"
      lastName isEqualTo "Smith"
      leadVisitor isEqualTo true
      assistedVisit isEqualTo false
      visitorNotes isEqualTo "visitor notes"
    }
  }

  @Test
  fun `should fail when unknown prison visit slot id`() {
    webTestClient.badRequest(
      CreateOfficialVisitRequest(
        prisonCode = PENTONVILLE_PRISONER.prison,
        prisonerNumber = PENTONVILLE_PRISONER.number,
        prisonVisitSlotId = -99,
        visitDate = today().plusDays(1),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        dpsLocationId = UUID.randomUUID(),
        visitTypeCode = "IN_PERSON",
        privateNotes = "private notes",
        publicNotes = "public notes",
      ),
      "Prison visit slot with id -99 not found.",
    )
  }

  @Test
  fun `should fail when prisoner not at prison`() {
    webTestClient.badRequest(
      CreateOfficialVisitRequest(
        prisonCode = WANDSWORTH,
        prisonerNumber = PENTONVILLE_PRISONER.number,
        prisonVisitSlotId = 1,
        visitDate = today().plusDays(1),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        dpsLocationId = UUID.randomUUID(),
        visitTypeCode = "IN_PERSON",
        privateNotes = "private notes",
        publicNotes = "public notes",
      ),
      "Prisoner ${PENTONVILLE_PRISONER.number} not found at prison WWI",
    )
  }

  private fun WebTestClient.create(request: CreateOfficialVisitRequest) = this
    .post()
    .uri("/official-visit")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = PENTONVILLE_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
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
    .headers(setAuthorisation(username = PENTONVILLE_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isBadRequest
    .expectBody().jsonPath("$.userMessage").isEqualTo(errorMessage)
}
