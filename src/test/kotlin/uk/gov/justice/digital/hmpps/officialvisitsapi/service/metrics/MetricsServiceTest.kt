package uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import java.time.LocalTime

class MetricsServiceTest {
  private val telemetryService: TelemetryService = mock()
  private val metricsService = MetricsService(telemetryService)

  @Test
  fun `official visit OfficialVisitCreated Telemetry event is published and return positive hoursBeforeStartTime`() {
    // start time is always in two hours in future
    val visitInfo = VisitMetricInfo(
      source = Source.DPS,
      username = MOORLAND_PRISON_USER.username,
      prisonCode = MOORLAND,
      officialVisitId = 1,
      prisonerNumber = MOORLAND_PRISONER.number,
      numberOfVisitors = 1,
      locationType = null,
      startTime = LocalTime.now().plusHours(2),
    )
    metricsService.send(MetricsEvents.CREATE, visitInfo)
    val info = MetricsEvents.CREATE.event(visitInfo)
    with(info) {
      metrics().get("hoursBeforeStartTime") isEqualTo 2.0
      properties().get("official_visit_id") isEqualTo "1"
      metrics().get("number_of_visitors") isEqualTo 1.0
    }
    verify(telemetryService).track(info)
  }

  @Test
  fun `official visit OfficialVisitUpdated Telemetry event is published and return positive hoursBeforeStartTime`() {
    // start time is always in two hours in future
    val visitInfo = VisitMetricInfo(
      source = Source.DPS,
      username = MOORLAND_PRISON_USER.username,
      prisonCode = MOORLAND,
      officialVisitId = 1,
      prisonerNumber = MOORLAND_PRISONER.number,
      numberOfVisitors = 1,
      locationType = null,
      startTime = LocalTime.now().plusHours(2),
    )
    metricsService.send(MetricsEvents.AMEND, visitInfo)
    val info = MetricsEvents.AMEND.event(visitInfo)
    with(info) {
      metrics().get("hoursBeforeStartTime") isEqualTo 2.0
      properties().get("official_visit_id") isEqualTo "1"
    }
    verify(telemetryService).track(info)
  }

  @Test
  fun `official visit OfficialVisitCancelled Telemetry event is published and return positive hoursBeforeStartTime`() {
    // start time is always in two hours in future
    val visitInfo = VisitMetricInfo(
      source = Source.DPS,
      username = MOORLAND_PRISON_USER.username,
      prisonCode = MOORLAND,
      officialVisitId = 1,
      prisonerNumber = MOORLAND_PRISONER.number,
      numberOfVisitors = 1,
      locationType = null,
      startTime = LocalTime.now().plusHours(2),
    )
    metricsService.send(MetricsEvents.CANCEL, visitInfo)
    val info = MetricsEvents.CANCEL.event(visitInfo)
    with(info) {
      metrics().get("hoursBeforeStartTime") isEqualTo 2.0
      properties().get("official_visit_id") isEqualTo "1"
    }
    verify(telemetryService).track(info)
  }

  @Test
  fun `official visit OfficialVisitCompleted Telemetry event is published and return negative hoursAfterStartTime`() {
    // start time is in past
    val visitInfo = VisitMetricInfo(
      source = Source.DPS,
      username = MOORLAND_PRISON_USER.username,
      prisonCode = MOORLAND,
      officialVisitId = 1,
      prisonerNumber = MOORLAND_PRISONER.number,
      numberOfVisitors = 1,
      locationType = null,
      startTime = LocalTime.now().minusHours(2),
    )
    metricsService.send(MetricsEvents.COMPLETE, visitInfo)
    val info = MetricsEvents.COMPLETE.event(visitInfo)
    with(info) {
      metrics().get("hoursAfterStartTime") isEqualTo -2.0
      properties().get("official_visit_id") isEqualTo "1"
    }
    verify(telemetryService).track(info)
  }

  @Test
  fun `official visitor OfficialVisitorAdded Telemetry event is published when visitor is added for official visit`() {
    val visitorInfo = VisitorMetricInfo(
      source = Source.DPS,
      username = MOORLAND_PRISON_USER.username,
      prisonCode = MOORLAND,
      officialVisitId = 1,
      contactId = 1,
      officialVisitorId = 1,
    )
    metricsService.send(MetricsEvents.ADD_VISITOR, visitorInfo)
    val info = MetricsEvents.ADD_VISITOR.event(visitorInfo)
    with(info) {
      properties().get("official_visitor_id") isEqualTo "1"
      properties().get("contact_id") isEqualTo "1"
      properties().get("official_visit_id") isEqualTo "1"
    }
    verify(telemetryService).track(info)
  }
}
