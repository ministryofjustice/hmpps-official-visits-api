package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alerts.AlertsClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alertsapi.model.Alert
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.alertsapi.model.AlertCodeSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PrisonerAlertsCheckerTest {

  private val alertsClient: AlertsClient = mock()
  private val checker = PrisonerAlertsChecker(alertsClient)

  private val createdTime = LocalDateTime.now()

  private fun officialVisit(createdTime: LocalDateTime): OfficialVisitEntity = mock {
    whenever(it.prisonerNumber).thenReturn("A1234BC")
    whenever(it.createdTime).thenReturn(createdTime)
  }

  private fun alert(isActive: Boolean, createdAt: LocalDateTime, alertCode: String = "XIT"): Alert = Alert(
    alertUuid = UUID.randomUUID(),
    prisonNumber = "A1234BC",
    alertCode = AlertCodeSummary("test-code", "test-description", alertCode, "test-type", true),
    activeFrom = LocalDate.now(),
    isActive = isActive,
    createdAt = createdAt,
    createdBy = "test-user",
    createdByDisplayName = "Test User",
  )

  @Test
  fun `returns PRISONER_NEW_ALERT when an active alert was created after the visit created date`() {
    val officialVisit = officialVisit(createdTime = createdTime)
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(alert(isActive = true, createdAt = createdTime.plusDays(1))),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
  }

  @Test
  fun `returns null when no alerts exist`() {
    val officialVisit = officialVisit(createdTime = createdTime)
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(emptyList())

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isNull()
  }

  @Test
  fun `returns null when alerts exist but none are active`() {
    val officialVisit = officialVisit(createdTime = createdTime)
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(alert(isActive = false, createdAt = createdTime.plusDays(1))),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isNull()
  }

  @Test
  fun `returns null when active alert was created before the visit created date`() {
    val officialVisit = officialVisit(createdTime = createdTime)
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(alert(isActive = true, createdAt = createdTime.minusDays(1))),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isNull()
  }

  @Test
  fun `returns null when active alert was created exactly at the visit created date`() {
    val officialVisit = officialVisit(createdTime = createdTime)
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(alert(isActive = true, createdAt = createdTime)),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isNull()
  }

  @Test
  fun `returns null when active alert was not one of the relevant alerts`() {
    val officialVisit = officialVisit(createdTime = createdTime)
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(alert(isActive = true, createdAt = createdTime, "EBG")),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isNull()
  }

  @Test
  fun `returns PRISONER_NEW_ALERT when at least one of several alerts qualifies`() {
    val officialVisit = officialVisit(createdTime = createdTime)
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(
        alert(isActive = false, createdAt = createdTime.plusDays(1)),
        alert(isActive = true, createdAt = createdTime.minusDays(1)),
        alert(isActive = true, createdAt = createdTime.plusHours(1)),
      ),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
  }
}
