package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import java.util.UUID

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

  fun stubGetOfficialVisitLocationsAtPrison(
    prisonCode: String = WANDSWORTH,
    locations: List<Location> = emptyList(),
    serviceType: String = "OFFICIAL_VISITS",
  ) {
    stubFor(
      get("/locations/non-residential/prison/$prisonCode/service/$serviceType?sortByLocalName=true&formatLocalName=true&filterParents=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(locations))
            .withStatus(200),
        ),
    )
  }

  fun stubLocationsByKeys(locationIds: List<UUID>, locationsToReturn: List<Location>) {
    stubFor(
      post("/locations/keys")
        .withRequestBody(equalToJson(mapper.writeValueAsString(locationIds.map { it.toString() })))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(locationsToReturn))
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
