package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AuditedEventResponse(
  @Schema(description = "The audited event identifier", example = "1")
  val auditedEventId: Long,

  @Schema(description = "The official visit identifier", example = "1")
  val officialVisitId: Long,

  @Schema(description = "The source of the auditing event", example = "DPS", allowableValues = ["DPS", "NOMIS"])
  val eventSource: String,

  @Schema(description = "A short summary of the audit event", example = "Visit updated")
  val eventSummary: String,

  @Schema(description = "The type of audit event", example = "UPDATE", allowableValues = ["CREATE", "UPDATE"])
  val eventType: String,

  @Schema(description = "The changes related to an update, otherwise empty", example = "[{\"field\":\"start_time\",\"oldValue\":\"12:00\",\"newValue\":\"17:00\"},{\"field\":\"end_time\",\"oldValue\":\"14:00\",\"newValue\":\"19:00\"}]")
  val eventChanges: List<AuditedEventChange> = emptyList(),

  @Schema(description = "The date and time the audited event was recorded", example = "2026-05-04 09:50")
  val eventDateTime: LocalDateTime,

  @Schema(description = "The username of the user responsible for the audited event", example = "X999X")
  val eventUsername: String,

  @Schema(description = "The full name of the user responsible for the audited event", example = "Fred Bloggs")
  val eventUserFullName: String,
) {
  @Schema(
    description =
    """
    A boolean indicator to determine if the audited event is considered a significant change to the official visit.
    """,
    example = "true",
  )
  val significantChange: Boolean = eventChanges.any(AuditedEventChange::significantChange)
}

data class AuditedEventChange(
  @Schema(description = "The name of the field affected by the audited event", example = "start_time")
  val field: String,

  @Schema(description = "The old value of the field affected by the audited event", example = "12:00")
  val oldValue: String?,

  @Schema(description = "The new value of the field affected by the audited event", example = "17:00")
  val newValue: String?,

  @JsonIgnore
  val significantChange: Boolean,
)
