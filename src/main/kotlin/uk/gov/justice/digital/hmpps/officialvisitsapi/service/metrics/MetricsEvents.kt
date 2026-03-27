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
  SEARCH("OfficialVisitSearch") {
    override fun event(additionalInformation: MetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  ADD_VISITOR("OfficialVisitorAdded") {
    override fun event(additionalInformation: MetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  REMOVE_VISITOR("OfficialVisitorRemoved") {
    override fun event(additionalInformation: MetricInfo) = OfficialVisitMetricTelemetry(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )
  },
  TIMESLOT_ADDED("TimeSlotAdded") {
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
      "username" to additionalInformation.username,
    )
    return when (additionalInformation) {
      is VisitMetricInfo -> {
        baseMap + additionalInformation.visitAdditionalInfo()
      }
      is SearchInfo -> {
        baseMap + additionalInformation.searchAdditionalInfo()
      }

      is VisitorMetricInfo -> {
        baseMap + additionalInformation.visitorAdditionalInfo()
      }

      is TimeSlotInfo -> {
        baseMap + additionalInformation.timeSLotInfo()
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
    else -> {
      emptyMap<String, Double>()
    }
  }

  fun VisitMetricInfo.hoursBeforeStartTimeMetric(): Pair<String, Double> = "hoursBeforeStartTime" to ChronoUnit.HOURS.between(
    LocalTime.now().withSecond(0).withNano(0),
    startTime?.withSecond(0)?.withNano(0),
  ).toDouble()

  fun VisitMetricInfo.hoursAfterStartTimeTimeMetrics(): Pair<String, Double> = "hoursAfterStartTime" to
    ChronoUnit.HOURS.between( startTime?.withSecond(0)?.withNano(0), LocalTime.now().withSecond(0).withNano(0),).toDouble()

  fun VisitMetricInfo.numberOfVisitors(): Pair<String, Double> = "number_of_visitors" to numberOfVisitors.toDouble()
}

private fun VisitMetricInfo.visitAdditionalInfo(): Map<String, String> = mapOf(
  "prisoner_number" to prisonerNumber,
  "official_visit_id" to "$officialVisitId",
)

private fun VisitorMetricInfo.visitorAdditionalInfo(): Map<String, String> = mapOf(
  "official_visitor_id" to "$officialVisitorId",
  "contact_id" to "$contactId",
  "official_visit_id" to "$officialVisitId",
)

private fun TimeSlotInfo.timeSLotInfo(): Map<String, String> = mapOf(
  "day_code" to dayCode,
)
private fun OfficialVisitMetricTelemetry.visitMetrics(
  additionalInformation: VisitMetricInfo,
): Map<String, Double> = listOfNotNull(
  additionalInformation.hoursBeforeStartTimeMetric()
    .takeIf { eventType == MetricsEvents.CREATE.eventType || eventType == MetricsEvents.AMEND.eventType || eventType == MetricsEvents.CANCEL.eventType },
  additionalInformation.numberOfVisitors().takeIf { eventType == MetricsEvents.CREATE.eventType },
  additionalInformation.hoursAfterStartTimeTimeMetrics()
    .takeIf { eventType == MetricsEvents.COMPLETE.eventType },
).toMap()

private fun SearchInfo.searchAdditionalInfo(): Map<String, String> = mapOf(
  "start_date" to "$startDate",
  "end_date" to "$endDate",
  "search_term" to searchTerm.orEmpty(),
  "visit_types" to "$visitTypes",
  "visit_statuses" to "$visitStatuses",
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
  override val prisonCode: String,
  override val username: String,
  val startDate: LocalDate,
  val searchTerm: String? = null,
  val endDate: LocalDate,
  val visitTypes: List<VisitType>?,
  val locationIds: List<UUID>?,
  val visitStatuses: List<VisitStatusType>?,
  val numberOfResults: Int = 0,
) : MetricInfo(username = username, prisonCode = prisonCode)

data class VisitorMetricInfo(
  override val source: Source = Source.DPS,
  override val username: String,
  override val prisonCode: String,
  val officialVisitId: Long,
  val contactId: Long,
  val officialVisitorId: Long,
) : MetricInfo(source = source, username = username, prisonCode = prisonCode)

data class TimeSlotInfo(
  override val source: Source = Source.DPS,
  override val username: String,
  override val prisonCode: String,
  val dayCode: String,
) : MetricInfo(source = source, username = username, prisonCode = prisonCode)
