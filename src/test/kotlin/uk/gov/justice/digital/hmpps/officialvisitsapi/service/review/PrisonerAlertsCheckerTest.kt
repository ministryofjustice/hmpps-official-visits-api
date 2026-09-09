package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
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

  private val referenceDate = LocalDateTime.of(2024, 1, 1, 12, 0)

  private fun officialVisit(updatedTime: LocalDateTime?, createdTime: LocalDateTime): OfficialVisitEntity = mock {
    whenever(it.prisonerNumber).thenReturn("A1234BC")
    whenever(it.updatedTime).thenReturn(updatedTime)
    whenever(it.createdTime).thenReturn(createdTime)
  }

  private fun alert(isActive: Boolean, createdAt: LocalDateTime): Alert = Alert(
    alertUuid = UUID.randomUUID(),
    prisonNumber = "A1234BC",
    alertCode = mock<AlertCodeSummary>(),
    activeFrom = LocalDate.now(),
    isActive = isActive,
    createdAt = createdAt,
    createdBy = "test-user",
    createdByDisplayName = "Test User",
  )

  @Nested
  inner class ReferenceDateSelection {

    @Test
    fun `uses updatedTime as reference date when present`() {
      val officialVisit = officialVisit(updatedTime = referenceDate, createdTime = referenceDate.minusDays(5))
      whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
        listOf(alert(isActive = true, createdAt = referenceDate.plusMinutes(1))),
      )

      val result = checker.checkPrisonerAlerts(officialVisit)

      assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
    }

    @Test
    fun `falls back to createdTime when updatedTime is null`() {
      val officialVisit = officialVisit(updatedTime = null, createdTime = referenceDate)
      whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
        listOf(alert(isActive = true, createdAt = referenceDate.plusMinutes(1))),
      )

      val result = checker.checkPrisonerAlerts(officialVisit)

      assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
    }
  }

  @Nested
  inner class AlertEvaluation {

    @Test
    fun `returns PRISONER_NEW_ALERT when an active alert was created after the reference date`() {
      val officialVisit = officialVisit(updatedTime = referenceDate, createdTime = referenceDate)
      whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
        listOf(alert(isActive = true, createdAt = referenceDate.plusDays(1))),
      )

      val result = checker.checkPrisonerAlerts(officialVisit)

      assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
    }

    @Test
    fun `returns null when no alerts exist`() {
      val officialVisit = officialVisit(updatedTime = referenceDate, createdTime = referenceDate)
      whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(emptyList())

      val result = checker.checkPrisonerAlerts(officialVisit)

      assertThat(result).isNull()
    }

    @Test
    fun `returns null when alerts exist but none are active`() {
      val officialVisit = officialVisit(updatedTime = referenceDate, createdTime = referenceDate)
      whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
        listOf(alert(isActive = false, createdAt = referenceDate.plusDays(1))),
      )

      val result = checker.checkPrisonerAlerts(officialVisit)

      assertThat(result).isNull()
    }

    @Test
    fun `returns null when active alert was created before the reference date`() {
      val officialVisit = officialVisit(updatedTime = referenceDate, createdTime = referenceDate)
      whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
        listOf(alert(isActive = true, createdAt = referenceDate.minusDays(1))),
      )

      val result = checker.checkPrisonerAlerts(officialVisit)

      assertThat(result).isNull()
    }

    @Test
    fun `returns null when active alert was created exactly at the reference date`() {
      val officialVisit = officialVisit(updatedTime = referenceDate, createdTime = referenceDate)
      whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
        listOf(alert(isActive = true, createdAt = referenceDate)),
      )

      val result = checker.checkPrisonerAlerts(officialVisit)

      assertThat(result).isNull()
    }

    @Test
    fun `returns PRISONER_NEW_ALERT when at least one of several alerts qualifies`() {
      val officialVisit = officialVisit(updatedTime = referenceDate, createdTime = referenceDate)
      whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
        listOf(
          alert(isActive = false, createdAt = referenceDate.plusDays(1)),
          alert(isActive = true, createdAt = referenceDate.minusDays(1)),
          alert(isActive = true, createdAt = referenceDate.plusHours(1)),
        ),
      )

      val result = checker.checkPrisonerAlerts(officialVisit)

      assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
    }
  }
}
