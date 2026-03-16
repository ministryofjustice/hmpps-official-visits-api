package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.TelemetryService

@Service
class OfficialVisitMetricTelemetryService(private val telemetryService: TelemetryService) {
  fun send(
    eventType: MetricsEvents,
    action: OVActions,
    info: VisitMetricInfo,
  ) {
    telemetryService.track(eventType.event(additionalInformation = info, action))
  }
}
