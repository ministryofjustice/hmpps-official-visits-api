package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alerts.AlertsClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity

@Service
class PrisonerAlertsChecker(private val alertsClient: AlertsClient) {

  fun checkPrisonerAlerts(officialVisit: OfficialVisitEntity, prisoner: Prisoner): IssueType? {
    val referenceDate = officialVisit.updatedTime ?: officialVisit.createdTime

    val hasNewAlert = alertsClient.getPrisonerAlerts(prisoner.prisonerNumber)
      .any { it.isActive == true && it.createdAt.isAfter(referenceDate) }

    return IssueType.PRISONER_NEW_ALERT.takeIf { hasNewAlert }
  }
}
