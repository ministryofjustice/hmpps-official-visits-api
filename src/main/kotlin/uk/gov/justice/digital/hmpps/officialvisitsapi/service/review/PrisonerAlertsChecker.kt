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
      "SA"
    )
  }

  fun checkPrisonerAlerts(officialVisit: OfficialVisitEntity): IssueType? {
    // TODO revisit this
    // Due to the timing of how this being called (via periodic job) we can miss alerts that have been created prior to the visit being updated.
    // For example a visit is created but there are no active alerts. An alert is created the visit is then subsequently updated after this and then the job to call this runs. The update will be missed.
    val referenceDate = officialVisit.updatedTime ?: officialVisit.createdTime

    val hasNewAlert = alertsClient.getPrisonerAlerts(officialVisit.prisonerNumber)
      .any { RELEVANT_ALERTS.contains(it.alertCode.code) && it.isActive && it.createdAt.isAfter(referenceDate) }

    return IssueType.PRISONER_NEW_ALERT.takeIf { hasNewAlert }
  }
}
