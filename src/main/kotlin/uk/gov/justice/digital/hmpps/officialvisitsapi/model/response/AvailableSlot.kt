package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class AvailableSlot(
  @Schema(description = "The prison visit slot identifier for the official visit slot", example = "1")
  val visitSlotId: Long,

  @Schema(description = "The prison time slot identifier for the official visit slot", example = "1")
  val timeSlotId: Long,

  @Schema(description = "The prison code where the visit takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "The 3-letter day of the week code where this slot falls in this prison.", example = "TUE")
  val dayCode: String,

  @Schema(description = "The day of the week code where this slot falls in this prison.", example = "Tuesday")
  val dayDescription: String,

  @Schema(description = "The date for the official visit slot")
  val visitDate: LocalDate,

  @Schema(description = "The start time for the official visit slot")
  val startTime: LocalTime,

  @Schema(description = "The end time for the official visit slot")
  val endTime: LocalTime,

  @Schema(description = "The DPS location where the official visit takes place for the slot", example = "aaaa-bbbb-xxxxxxxx-yyyyyyyy")
  val dpsLocationId: UUID,

  @Schema(description = "The available video sessions for official visit slot")
  val availableVideoSessions: Int,

  @Schema(description = "The available adults for official visit slot")
  val availableAdults: Int,

  @Schema(description = "The available groups for official visit slot")
  val availableGroups: Int,

  @Schema(description = "The description of the location where this slot exists", example = "Legal visits room 8")
  var locationDescription: String? = null,
)
