package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.time.LocalTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "v_available_visit_slots")
data class AvailableSlotEntity(
  @Id
  val prisonVisitSlotId: Long,

  val prisonTimeSlotId: Long,

  val prisonCode: String,

  val displaySequence: Int,

  val dayCode: String,

  val dayDescription: String,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val dpsLocationId: UUID,

  val maxAdults: Int,

  val maxGroups: Int,

  val maxVideoSessions: Int,
)
