package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.MetricTelemetryEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.TelemetryService
import java.time.LocalTime

@Service
class OfficialVisitMetricTelemetryService(private val telemetryService: TelemetryService) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
  fun send(
    eventType: String,
    info: VisitMetricInfo,
  ) {
    telemetryService.track(OfficialVisitMetricTelemetry(eventType, info))
  }
}

data class OfficialVisitMetricTelemetry(
  override val eventType: String,
  val additionalInformation: VisitMetricInfo,
) : MetricTelemetryEvent(eventType) {
  override fun properties() = mapOf(
    "official_visit_id" to "$additionalInformation.officialVisitId",
    "prison_code" to additionalInformation.prisonCode,
    "prisoner_number" to additionalInformation.prisonerNumber,
    "username" to additionalInformation.username,
    "source" to additionalInformation.source.toString(),
  )
  override fun metrics() = mapOf(additionalInformation.hoursBeforeStartTimeMetric(), additionalInformation.numberOfVisitors())
}

data class VisitMetricInfo(
  val source: Source = Source.DPS,
  val username: String,
  val officialVisitId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val numberOfVisitors: Long = 0,
  val locationType: String? = null,
  val startTime: LocalTime,
  val endTime: LocalTime,
)
