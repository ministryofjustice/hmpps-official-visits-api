package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alerts.AlertsClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity

@Service
class PrisonerAlertsChecker(private val alertsClient: AlertsClient) {

  companion object {
    private val RELEVANT_ALERTS = setOf(
      "MFL",
      "MHT",
      "MSI",
      "MSP",
      "PEEP",
      "OHCO",
      "RCDR",
      "RCON",
      "RDV",
      "RKC",
      "RKS",
      "RSP",
      "RNO121",
      "RPB",
      "RPC",
      "RSS",
      "RST",
      "SO",
      "SONR",
      "SOR",
      "SR",
      "SSHO",
      "V45",
      "V46",
      "VJOP",
      "VOP",
      "XRF",
      "XSO",
      "XECV",
      "XCCI",
      "XIT",
      "SA",
    )
  }

  fun checkPrisonerAlerts(officialVisit: OfficialVisitEntity): IssueType? {
    val hasNewAlert = alertsClient.getPrisonerAlerts(officialVisit.prisonerNumber)
      .any {
        RELEVANT_ALERTS.contains(it.alertCode.code) &&
          it.isActive &&
          it.activeFrom <= officialVisit.visitDate &&
          (it.activeTo == null || it.activeTo >= officialVisit.visitDate)
      }

    return IssueType.PRISONER_NEW_ALERT.takeIf { hasNewAlert }
  }
}
