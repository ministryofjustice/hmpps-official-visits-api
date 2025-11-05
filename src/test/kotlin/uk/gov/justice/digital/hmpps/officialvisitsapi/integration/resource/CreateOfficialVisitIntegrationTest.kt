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
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository

class CreateOfficialVisitIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var officialVisitRepository: OfficialVisitRepository

  @Test
  @Transactional
  fun `should create official visit with one social visitor`() {
    val officialVisitResponse = webTestClient.create(
      CreateOfficialVisitRequest(
        prisonerNumber = PENTONVILLE_PRISONER.number,
        prisonVisitSlotId = 1,
        prisonCode = PENTONVILLE_PRISONER.prison,
        visitDate = today(),
        visitTypeCode = "IN_PERSON",
        officialVisitors = listOf(
          OfficialVisitor(
            visitorTypeCode = "CONTACT",
            contactTypeCode = "SOCIAL",
            contactId = null,
            prisonerContactId = null,
          ),
        ),
      ),
    )

    val persistedOfficialVisit = officialVisitRepository.findById(officialVisitResponse.officialVisitId).get()

    with(persistedOfficialVisit) {
      prisonVisitSlot.prisonVisitSlotId isEqualTo 1
      prisonerNumber isEqualTo PENTONVILLE_PRISONER.number
      prisonCode isEqualTo PENTONVILLE_PRISONER.prison
      visitDate isEqualTo today()
      visitTypeCode isEqualTo "IN_PERSON"
      visitStatusCode isEqualTo "ACTIVE"
      createdBy isEqualTo PENTONVILLE_PRISON_USER.username
      createdTime isCloseTo now()
    }

    with(persistedOfficialVisit.officialVisitors().single()) {
      visitorTypeCode isEqualTo "CONTACT"
      contactTypeCode isEqualTo "SOCIAL"
    }
  }

  @Test
  fun `should fail when unknown prison visit slot id`() {
    webTestClient.badRequest(
      CreateOfficialVisitRequest(
        prisonerNumber = PENTONVILLE_PRISONER.number,
        prisonVisitSlotId = -99,
        prisonCode = PENTONVILLE_PRISONER.prison,
        visitDate = today(),
        visitTypeCode = "IN_PERSON",
      ),
      "Prison visit slot with id -99 not found.",
    )
  }

  @Test
  fun `should fail when prisoner not at prison`() {
    webTestClient.badRequest(
      CreateOfficialVisitRequest(
        prisonerNumber = PENTONVILLE_PRISONER.number,
        prisonVisitSlotId = 1,
        prisonCode = WANDSWORTH,
        visitDate = today(),
        visitTypeCode = "IN_PERSON",
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
