package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.NonResidentialSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH

class LocationsInsidePrisonApiMockServer : MockServer(8091) {

  fun stubGetLocationById(location: Location) {
    stubFor(
      get("/locations/${location.id}?formatLocalName=true").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(location))
          .withStatus(200),
      ),
    )
  }

  fun stubGetNonResidentialOfficialVisitLocationsAtPrison(
    prisonCode: String = WANDSWORTH,
    locationSummary: NonResidentialSummary,
  ) {
    stubFor(
      get("/locations/non-residential/summary/$prisonCode?status=ACTIVE&locationType=VISITS&serviceType=OFFICIAL_VISITS&sortByLocalName=true&formatLocalName=true&page=0&pageSize=200")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(locationSummary))
            .withStatus(200),
        ),
    )
  }
}

class LocationsInsidePrisonApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = LocationsInsidePrisonApiMockServer()
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
