package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class AvailableSlot(
  val visitSlotId: Long,
  val timeSlotId: Long,
  val prisonCode: String,
  val dayCode: String,
  val dayDescription: String,
  val visitDate: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val dpsLocationId: UUID,
  val availableVideoSessions: Int,
  val availableAdults: Int,
  val availableGroups: Int,
)
