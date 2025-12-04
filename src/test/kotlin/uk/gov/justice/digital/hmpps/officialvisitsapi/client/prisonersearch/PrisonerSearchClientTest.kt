package uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock.PrisonerSearchApiMockServer

class PrisonerSearchClientTest {

  private val server = PrisonerSearchApiMockServer().also { it.start() }
  private val client = PrisonerSearchClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching prisoner`() {
    server.stubGetPrisoner(PENTONVILLE_PRISONER)

    client.getPrisoner(PENTONVILLE_PRISONER.number) isEqualTo prisonerSearchPrisoner(prisonerNumber = PENTONVILLE_PRISONER.number, prisonCode = PENTONVILLE_PRISONER.prison, bookingId = PENTONVILLE_PRISONER.bookingId)
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}
