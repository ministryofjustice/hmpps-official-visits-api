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

  private val today = LocalDateTime.now()

  private fun officialVisit(visitDate: LocalDate?, createdTime: LocalDateTime): OfficialVisitEntity = mock {
    whenever(it.prisonerNumber).thenReturn("A1234BC")
    whenever(it.visitDate).thenReturn(visitDate)
    whenever(it.createdTime).thenReturn(createdTime)
  }

  private fun alert(
    isActive: Boolean,
    createdAt: LocalDateTime,
    alertCode: String = "XIT",
    activeFrom: LocalDate,
    activeTo: LocalDate?,
  ): Alert = Alert(
    alertUuid = UUID.randomUUID(),
    prisonNumber = "A1234BC",
    alertCode = AlertCodeSummary("test-code", "test-description", alertCode, "test-type", true),
    activeFrom = activeFrom,
    isActive = isActive,
    activeTo = activeTo,
    createdAt = createdAt,
    createdBy = "test-user",
    createdByDisplayName = "Test User",
  )

  @Test
  fun `returns PRISONER_NEW_ALERT when the alert is active before the visit date and remains active on the visit date`() {
    val officialVisit = officialVisit(visitDate = LocalDate.now(), createdTime = LocalDateTime.now().minusDays(1))
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(
        alert(
          isActive = true,
          createdAt = today.plusDays(1),
          activeFrom = LocalDate.now().minusDays(2),
          activeTo = LocalDate.now().plusDays(7),
        ),
      ),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
  }

  @Test
  fun `returns null when no alerts exist`() {
    val officialVisit = officialVisit(visitDate = LocalDate.now(), createdTime = LocalDateTime.now().minusDays(1))
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(emptyList())

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isNull()
  }

  @Test
  fun `returns null when alerts exist but none are active`() {
    val officialVisit = officialVisit(visitDate = LocalDate.now(), createdTime = LocalDateTime.now().minusDays(1))
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(
        alert(
          isActive = false,
          createdAt = LocalDateTime.now().plusDays(1),
          activeFrom = LocalDate.now().minusDays(2),
          activeTo = LocalDate.now().plusDays(7),
        ),
      ),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isNull()
  }

  @Test
  fun `returns PRISONER_NEW_ALERT when the alert starts on the visit date`() {
    val officialVisit = officialVisit(visitDate = LocalDate.now(), createdTime = LocalDateTime.now())
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(
        alert(
          isActive = true,
          createdAt = today.minusDays(1),
          activeFrom = LocalDate.now(),
          activeTo = LocalDate.now().plusDays(7),
        ),
      ),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
  }

  @Test
  fun `returns PRISONER_NEW_ALERT when the alert ends on the visit date`() {
    val officialVisit = officialVisit(visitDate = LocalDate.now(), createdTime = LocalDateTime.now())
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(
        alert(
          isActive = true,
          createdAt = today,
          activeFrom = LocalDate.now().minusDays(2),
          activeTo = LocalDate.now(),
        ),
      ),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
  }

  @Test
  fun `returns PRISONER_NEW_ALERT when the alert has no end date`() {
    val officialVisit = officialVisit(visitDate = LocalDate.now(), createdTime = LocalDateTime.now())
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(
        alert(
          isActive = true,
          createdAt = today,
          activeFrom = LocalDate.now().minusDays(2),
          activeTo = null,
        ),
      ),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
  }

  @Test
  fun `returns null when active alert was not one of the relevant alerts`() {
    val officialVisit = officialVisit(visitDate = LocalDate.now(), createdTime = LocalDateTime.now())
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(
        alert(
          isActive = true,
          createdAt = today,
          "EBG",
          LocalDate.now().minusDays(2),
          LocalDate.now().plusDays(7),
        ),
      ),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isNull()
  }

  @Test
  fun `returns PRISONER_NEW_ALERT when at least one of several alerts qualifies`() {
    val officialVisit = officialVisit(visitDate = LocalDate.now(), createdTime = LocalDateTime.now())
    whenever(alertsClient.getPrisonerAlerts("A1234BC")).thenReturn(
      listOf(
        alert(
          isActive = false,
          createdAt = today.plusDays(1),
          activeFrom = LocalDate.now().minusDays(2),
          activeTo = LocalDate.now().plusDays(7),
        ),
        alert(
          isActive = true,
          createdAt = today.minusDays(1),
          activeFrom = LocalDate.now().minusDays(2),
          activeTo = LocalDate.now().plusDays(7),
        ),
        alert(
          isActive = true,
          createdAt = today.plusHours(1),
          activeFrom = LocalDate.now().minusDays(2),
          activeTo = LocalDate.now().plusDays(7),
        ),
      ),
    )

    val result = checker.checkPrisonerAlerts(officialVisit)

    assertThat(result).isEqualTo(IssueType.PRISONER_NEW_ALERT)
  }
}
