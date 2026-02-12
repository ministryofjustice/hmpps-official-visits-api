package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PagedPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerNumbers
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner as SearchPrisoner

class PrisonerSearchApiMockServer : MockServer(8092) {

  fun stubGetPrisoner(prisoner: Prisoner) {
    stubFor(
      get("/prisoner/${prisoner.number}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                prisonerSearchPrisoner(
                  prisonerNumber = prisoner.number,
                  prisonCode = prisoner.prison,
                  bookingId = prisoner.bookingId,
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubFindPrisonersBySearchTerm(prisonCode: String, searchTerm: String, vararg prisoners: Prisoner) {
    stubFor(
      get("/prison/$prisonCode/prisoners?term=$searchTerm&page=0&size=200")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                PagedPrisoner(content = prisoners.toList().map { prisonerSearchPrisoner(prisonCode = prisonCode, prisonerNumber = it.number, bookingId = it.bookingId) }),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubGetPrisonName(prisonCode: String, vararg prisoner: Prisoner) {
    stubFor(
      get("/prison/$prisonCode/prisoners?page=0&size=1")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                PagedPrisoner(content = prisoner.toList().map { prisonerSearchPrisoner(prisonCode = prisonCode, prisonerNumber = it.number, bookingId = it.bookingId) }),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubSearchPrisonersByPrisonerNumbers(idsBeingSearchFor: List<String>, prisonersToReturn: List<SearchPrisoner>) {
    stubFor(
      post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbers(idsBeingSearchFor)), true, true))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(prisonersToReturn))
            .withStatus(200),
        ),
    )
  }
}

class PrisonerSearchApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = PrisonerSearchApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    server.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    server.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    server.stop()
  }
}
