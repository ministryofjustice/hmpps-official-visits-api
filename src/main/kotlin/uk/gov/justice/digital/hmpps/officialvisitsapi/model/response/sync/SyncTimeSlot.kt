package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "Sync response for a prison time slot")
data class SyncTimeSlot(
  @Schema(description = "Time slot ID", example = "18767")
  val prisonTimeSlotId: Long,

  @Schema(description = "Prison code", example = "MDI")
  val prisonCode: String,

  @Schema(description = "Day code MON-SUN", example = "MON")
  val dayCode: DayType,

  @Schema(description = "Start time", example = "09:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "End time", example = "11:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "Effective date. The date from which this time slot is active", example = "2026-01-21")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val effectiveDate: LocalDate,

  @Schema(description = "Expiry date. The date from which this time is no longer be considered active", example = "2027-01-21")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val expiryDate: LocalDate? = null,

  @Schema(description = "User who created the entry", example = "admin")
  val createdBy: String,

  @Schema(description = "The timestamp of when this slot was created", example = "2024-01-01T00:00:00Z")
  val createdTime: LocalDateTime = LocalDateTime.now(),

  @Schema(description = "User who last updated the entry", example = "admin")
  val updatedBy: String? = null,

  @Schema(description = "The timestamp of when this slot was last updated", example = "2024-01-01T00:00:00Z")
  val updatedTime: LocalDateTime? = null,
)
