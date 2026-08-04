package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.model.PrisonerNonAssociations

class NonAssociationsApiMockServer : MockServer(8095) {

  fun stubGetPrisonerNonAssociations(
    prisonerNumber: String,
    nonAssociations: PrisonerNonAssociations,
  ) {
    stubFor(
      getNonAssociationsFor(prisonerNumber).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(nonAssociations))
          .withStatus(200),
      ),
    )
  }

  fun stubGetPrisonerNonAssociationsNotFound(prisonerNumber: String) {
    stubFor(
      getNonAssociationsFor(prisonerNumber).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(404),
      ),
    )
  }

  private fun getNonAssociationsFor(prisonerNumber: String) = get(urlPathEqualTo("/prisoner/$prisonerNumber/non-associations"))
    .withQueryParam("includeOpen", equalTo("true"))
    .withQueryParam("includeClosed", equalTo("false"))
    .withQueryParam("includeOtherPrisons", equalTo("false"))
}

class NonAssociationsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = NonAssociationsApiMockServer()
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
