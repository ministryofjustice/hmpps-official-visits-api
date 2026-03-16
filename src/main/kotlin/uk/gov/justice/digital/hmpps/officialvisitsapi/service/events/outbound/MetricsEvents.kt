package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound

import uk.gov.justice.digital.hmpps.officialvisitsapi.service.MetricTelemetryEvent
import java.time.LocalTime

enum class MetricsEvents(val eventType: String) {
  VISIT_CREATED("official-visits-api.visit.created") {
    override fun event(additionalInformation: VisitMetricInfo, action: OVActions) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      action = action,
      additionalInformation = additionalInformation,
    )
  },
  VISIT_UPDATED("official-visits-api.visit.updated") {
    override fun event(additionalInformation: VisitMetricInfo, action: OVActions) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      action = action,
      additionalInformation = additionalInformation,
    )
  },
  VISIT_CANCELLED("official-visits-api.visit.cancelled") {
    override fun event(additionalInformation: VisitMetricInfo, action: OVActions) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      action = action,
      additionalInformation = additionalInformation,
    )
  }, ;

  abstract fun event(
    additionalInformation: VisitMetricInfo,
    action: OVActions,
  ): OfficialVisitMetricTelemetry
}

data class OfficialVisitMetricTelemetry(
  override val eventType: String,
  val action: OVActions,
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
      .takeIf { action == OVActions.CREATE || action == OVActions.AMEND || action == OVActions.CANCEL },
    additionalInformation.numberOfVisitors().takeIf { action == OVActions.CREATE },
    additionalInformation.hoursAfterStartTimeTimeMetrics()
      .takeIf { action == OVActions.COMPLETE },
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

enum class OVActions {
  CREATE,
  AMEND,
  CANCEL,
  COMPLETE,
}
