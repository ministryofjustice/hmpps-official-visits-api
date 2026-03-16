package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service

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
}

abstract class StandardTelemetryEvent(eventType: String) : TelemetryEvent(eventType)
