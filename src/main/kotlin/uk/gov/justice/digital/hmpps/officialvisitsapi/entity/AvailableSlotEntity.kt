package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.time.LocalDate
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

  val effectiveDate: LocalDate,

  val expiryDate: LocalDate? = null,

  val dpsLocationId: UUID,

  val maxAdults: Int? = 0,

  val maxGroups: Int? = 0,

  val maxVideoSessions: Int? = 0,
)
