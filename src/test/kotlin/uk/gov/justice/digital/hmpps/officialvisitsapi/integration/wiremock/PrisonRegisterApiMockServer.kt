package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.ContactDetailsDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.PrisonDto

class PrisonRegisterApiMockServer : MockServer(port = 8097) {
  fun stubGetPrisonDetails(prisonId: String, prisonDetails: PrisonDto) {
    stubFor(
      get("/prisons/id/$prisonId")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(prisonDetails))
            .withStatus(200),
        ),
    )
  }

  fun stubGetPrisonDetailsNotFound(prisonId: String) {
    stubFor(
      get("/prisons/id/$prisonId")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404),
        ),
    )
  }

  fun stubGetPrisonContactDetails(prisonId: String, departmentType: String, contactDetails: ContactDetailsDto) {
    stubFor(
      get("/secure/prisons/id/$prisonId/department/contact-details?departmentType=$departmentType")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(contactDetails))
            .withStatus(200),
        ),
    )
  }
}

class PrisonRegisterApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = PrisonRegisterApiMockServer()
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
