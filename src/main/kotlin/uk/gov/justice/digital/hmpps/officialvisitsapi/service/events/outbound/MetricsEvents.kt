package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound

import uk.gov.justice.digital.hmpps.officialvisitsapi.service.MetricTelemetryEvent
import java.time.LocalTime

enum class MetricsEvents(val eventType: String) {
  CREATE("CREATE") {
    override fun event(additionalInformation: VisitMetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  AMEND("AMEND") {
    override fun event(additionalInformation: VisitMetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  CANCEL("CANCEL") {
    override fun event(additionalInformation: VisitMetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  COMPLETE("COMPLETE") {
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
      .takeIf { eventType == "CREATE" || eventType == "AMEND" || eventType == "CANCEL" },
    additionalInformation.numberOfVisitors().takeIf { eventType == "CREATE" },
    additionalInformation.hoursAfterStartTimeTimeMetrics()
      .takeIf { eventType == "COMPLETE" },
  ).toMap()
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
