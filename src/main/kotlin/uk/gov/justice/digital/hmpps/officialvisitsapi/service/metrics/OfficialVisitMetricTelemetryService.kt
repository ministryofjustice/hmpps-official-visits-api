package uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.TelemetryService

@Service
class OfficialVisitMetricTelemetryService(private val telemetryService: TelemetryService) {
  fun send(
    eventType: MetricsEvents,
    info: MetricInfo,
  ) {
    telemetryService.track(eventType.event(additionalInformation = info))
  }
}
