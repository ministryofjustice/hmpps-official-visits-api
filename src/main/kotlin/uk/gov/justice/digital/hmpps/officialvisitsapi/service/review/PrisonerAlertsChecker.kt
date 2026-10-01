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
    /**
     * Due to the timing of how this being called (via periodic job) we can miss alerts that have been created
     * between the visit creation and visit update times. However, if you have updated the visit, the user will
     * have seen the in-service update journey warnings showing the new alert.
     * For example a visit is created when there are no active alerts, an alert is created and the visit is
     * subsequently updated, and then the scheduled job to check for issues runs. This will not be raised as an issue.
     */
    val referenceDate = officialVisit.updatedTime ?: officialVisit.createdTime

    val hasNewAlert = alertsClient.getPrisonerAlerts(officialVisit.prisonerNumber)
      .any { RELEVANT_ALERTS.contains(it.alertCode.code) && it.isActive && it.createdAt.isAfter(referenceDate) }

    return IssueType.PRISONER_NEW_ALERT.takeIf { hasNewAlert }
  }
}
