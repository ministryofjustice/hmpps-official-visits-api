package uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics

import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.MetricTelemetryEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.collections.plus

enum class MetricsEvents(val eventType: String) {
  CREATE("OfficialVisitCreated") {
    override fun event(additionalInformation: MetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  AMEND("OfficialVisitUpdated") {
    override fun event(additionalInformation: MetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  CANCEL("OfficialVisitCancelled") {
    override fun event(additionalInformation: MetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  COMPLETE("OfficialVisitCompleted") {
    override fun event(additionalInformation: MetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  SEARCH("OfficialVisitSearched") {
    override fun event(additionalInformation: MetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  ;

  abstract fun event(
    additionalInformation: MetricInfo,
  ): OfficialVisitMetricTelemetry
}

data class OfficialVisitMetricTelemetry(
  override val eventType: String,
  val additionalInformation: MetricInfo,
) : MetricTelemetryEvent(eventType) {

  override fun properties(): Map<String, String> {
    val baseMap = mapOf(
      "prison_code" to additionalInformation.prisonCode,
      "source" to additionalInformation.source.toString(),
    )
    return when (additionalInformation) {
      is VisitMetricInfo -> {
        baseMap + additionalInformation.visitAdditionalInfo()
      }

      is SearchInfo -> {
        baseMap + additionalInformation.searchAdditionalInfo()
      }
    }
  }

  override fun metrics(): Map<String, Double> = when (additionalInformation) {
    is VisitMetricInfo -> {
      visitMetrics(additionalInformation)
    }

    is SearchInfo -> {
      mapOf(
        "number_of_results" to additionalInformation.numberOfResults.toDouble(),
      )
    }
  }

  fun VisitMetricInfo.hoursBeforeStartTimeMetric(): Pair<String, Double> = "hoursBeforeStartTime" to ChronoUnit.HOURS.between(
    startTime,
    LocalTime.now(),
  ).toDouble()

  fun VisitMetricInfo.hoursAfterStartTimeTimeMetrics(): Pair<String, Double> = "hoursAfterStartTime" to ChronoUnit.HOURS.between(LocalTime.now(), startTime).toDouble()

  fun VisitMetricInfo.numberOfVisitors(): Pair<String, Double> = "number_of_visitors" to numberOfVisitors.toDouble()
}

private fun VisitMetricInfo.visitAdditionalInfo(): Map<String, String> = mapOf(
  "prisoner_number" to prisonerNumber,
  "username" to username,
)

private fun OfficialVisitMetricTelemetry.visitMetrics(
  additionalInformation: VisitMetricInfo,
): Map<String, Double> = listOfNotNull(
  additionalInformation.hoursBeforeStartTimeMetric()
    .takeIf { eventType == MetricsEvents.CREATE.eventType || eventType == MetricsEvents.AMEND.eventType || eventType == MetricsEvents.CANCEL.eventType },
  additionalInformation.numberOfVisitors().takeIf { eventType == MetricsEvents.CREATE.eventType },
  additionalInformation.hoursAfterStartTimeTimeMetrics()
    .takeIf { eventType == MetricsEvents.CREATE.eventType },
).toMap()

private fun SearchInfo.searchAdditionalInfo(): Map<String, String> = mapOf(
  "start_date" to "$startDate",
  "end_date" to "$endDate",
  "search_term" to searchTerm.orEmpty(),
  "visit_types" to "$visitTypes",
  "visit_statuses" to "visitStatuses",
  "location_Ids" to "$locationIds",
)

sealed class MetricInfo(
  open val source: Source = Source.DPS,
  open val username: String,
  open val prisonCode: String,
)

data class VisitMetricInfo(
  override val source: Source = Source.DPS,
  override val username: String,
  override val prisonCode: String,
  val officialVisitId: Long,
  val prisonerNumber: String,
  val numberOfVisitors: Long = 0,
  val locationType: String? = null,
  val startTime: LocalTime? = null,
) : MetricInfo(source = source, username = username, prisonCode = prisonCode)

data class SearchInfo(
  override val source: Source = Source.DPS,
  override val username: String,
  override val prisonCode: String,
  val startDate: LocalDate? = null,
  val searchTerm: String? = null,
  val endDate: LocalDate? = null,
  val visitTypes: List<VisitType>?,
  val locationIds: List<UUID>?,
  val visitStatuses: List<VisitStatusType>?,
  val numberOfResults: Int = 0,
) : MetricInfo(source = source, username = username, prisonCode = prisonCode)
