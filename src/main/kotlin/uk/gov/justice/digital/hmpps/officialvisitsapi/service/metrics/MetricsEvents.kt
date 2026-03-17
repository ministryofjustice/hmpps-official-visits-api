package uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics

import uk.gov.justice.digital.hmpps.officialvisitsapi.service.MetricTelemetryEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class MetricsEvents(val eventType: String) {
  CREATE("OfficialVisitCreated") {
    override fun event(additionalInformation: VisitMetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  AMEND("OfficialVisitUpdated") {
    override fun event(additionalInformation: VisitMetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  CANCEL("OfficialVisitCancelled") {
    override fun event(additionalInformation: VisitMetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  COMPLETE("OfficialVisitCompleted") {
    override fun event(additionalInformation: VisitMetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  ;

  abstract fun event(
    additionalInformation: VisitMetricInfo,
  ): OfficialVisitMetricTelemetry
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

  // hoursBeforeStartTime is only applicable for update/create
  // numberOfVisitors is only applicable for Create
  override fun metrics() = listOfNotNull(
    additionalInformation.hoursBeforeStartTimeMetric()
      .takeIf { eventType == MetricsEvents.CREATE.eventType || eventType == MetricsEvents.AMEND.eventType || eventType == MetricsEvents.CANCEL.eventType },
    additionalInformation.numberOfVisitors().takeIf { eventType == MetricsEvents.CREATE.eventType },
    additionalInformation.hoursAfterStartTimeTimeMetrics()
      .takeIf { eventType == MetricsEvents.CREATE.eventType },
  ).toMap()

  fun VisitMetricInfo.hoursBeforeStartTimeMetric(): Pair<String, Double> = "hoursBeforeStartTime" to ChronoUnit.HOURS.between(
    startTime,
    LocalTime.now(),
  ).toDouble()

  fun VisitMetricInfo.hoursAfterStartTimeTimeMetrics(): Pair<String, Double> = "hoursAfterStartTime" to ChronoUnit.HOURS.between(LocalTime.now(), startTime).toDouble()

  fun VisitMetricInfo.numberOfVisitors(): Pair<String, Double> = "number_of_visitors" to numberOfVisitors.toDouble()
}

data class VisitMetricInfo(
  val source: Source = Source.DPS,
  val username: String,
  val officialVisitId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val numberOfVisitors: Long = 0,
  val locationType: String? = null,
  val startTime: LocalTime? = null,
)
