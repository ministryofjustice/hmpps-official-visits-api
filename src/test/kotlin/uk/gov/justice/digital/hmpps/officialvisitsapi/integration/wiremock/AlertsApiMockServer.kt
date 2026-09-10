package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alertsapi.model.Alert
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alertsapi.model.PageAlert
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Prisoner

class AlertsApiMockServer : MockServer(8096) {
  fun stubGetPrisonerAlerts(prisoner: Prisoner, vararg alerts: Alert) {
    stubGetPrisonerAlerts(prisoner.number, alerts.toList())
  }

  fun stubGetPrisonerAlerts(
    prisonerNumber: String,
    alerts: List<Alert>,
  ) {
    stubFor(
      getAlertsFor(prisonerNumber).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(PageAlert(content = alerts)))
          .withStatus(200),
      ),
    )
  }

  fun stubGetPrisonerAlertsNotFound(prisonerNumber: String) {
    stubFor(
      getAlertsFor(prisonerNumber).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(404),
      ),
    )
  }

  private fun getAlertsFor(prisonerNumber: String) = get(urlPathEqualTo("/prisoner/$prisonerNumber/alerts"))
}

class AlertsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = AlertsApiMockServer()
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
