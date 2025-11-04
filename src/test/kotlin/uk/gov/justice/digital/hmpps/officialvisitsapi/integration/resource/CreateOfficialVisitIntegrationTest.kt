package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import java.time.LocalDate
import java.time.LocalDateTime

class CreateOfficialVisitIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var officialVisitRepository: OfficialVisitRepository

  @Test
  fun `happy path - create official visit`() {
    val officialVisitResponse = webTestClient.create(
      CreateOfficialVisitRequest(
        prisonerNumber = "1234567",
        prisonVisitSlotId = 1,
        prisonCode = "PVI",
        visitDate = LocalDate.now(),
        visitTypeCode = "IN_PERSON",
      ),
    )

    with(officialVisitRepository.findById(officialVisitResponse.officialVisitId).get()) {
      prisonVisitSlot.prisonVisitSlotId isEqualTo 1
      prisonerNumber isEqualTo "1234567"
      prisonCode isEqualTo "PVI"
      visitDate isEqualTo LocalDate.now()
      visitTypeCode isEqualTo "IN_PERSON"
      visitStatusCode isEqualTo "ACTIVE"
      createdBy isEqualTo PRISON_USER.username
      createdTime isCloseTo LocalDateTime.now()
    }
  }

  private fun WebTestClient.create(request: CreateOfficialVisitRequest) = this
    .post()
    .uri("/official-visit")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(CreateOfficialVisitResponse::class.java)
    .returnResult().responseBody!!
}
