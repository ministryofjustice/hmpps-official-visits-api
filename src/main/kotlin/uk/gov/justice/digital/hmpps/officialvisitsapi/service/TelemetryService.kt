package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitMetricInfo
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Service
class TelemetryService(private val telemetryClient: TelemetryClient) {
  fun track(event: TelemetryEvent) {
    when (event) {
      is MetricTelemetryEvent -> telemetryClient.trackEvent(event.eventType, event.properties(), event.metrics())
      is StandardTelemetryEvent -> telemetryClient.trackEvent(event.eventType, event.properties(), null)
    }
  }
}

sealed class TelemetryEvent(open val eventType: String) {
  abstract fun properties(): Map<String, String>
}

abstract class MetricTelemetryEvent(eventType: String) : TelemetryEvent(eventType) {
  abstract fun metrics(): Map<String, Double>

  protected fun VisitMetricInfo.hoursBeforeStartTimeMetric(): Pair<String, Double> = "hoursBeforeStartTime" to ChronoUnit.HOURS.between(
    startTime,
    LocalTime.now(),
  ).toDouble()

  protected fun VisitMetricInfo.hoursAfterStartTimeTimeMetrics(): Pair<String, Double> = "hoursAfterStartTime" to ChronoUnit.HOURS.between(LocalTime.now(), startTime).toDouble()

  protected fun VisitMetricInfo.numberOfVisitors(): Pair<String, Double> = "number_of_visitors" to numberOfVisitors.toDouble()
}

abstract class StandardTelemetryEvent(eventType: String) : TelemetryEvent(eventType)
