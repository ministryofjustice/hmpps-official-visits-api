package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.pagedModelPrisonerContactSummary

class PersonalRelationshipsApiMockServer : MockServer(8094) {
  fun stubApprovedContacts(prisonerNumber: String) {
    stubFor(
      get(urlPathEqualTo("/prisoner/$prisonerNumber/contact"))
        .withQueryParam("relationshipType", equalTo("O"))
        .withQueryParam( "active", equalTo("true"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(pagedModelPrisonerContactSummary(prisonerNumber, "O")))
            .withStatus(200),
        ),
    )
  }
}

class PersonalRelationshipsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = PersonalRelationshipsApiMockServer()
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
