package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "prison_time_slot")
data class PrisonTimeSlotEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonTimeSlotId: Long = 0,

  val prisonCode: String,

  @Enumerated(EnumType.STRING)
  val dayCode: DayType,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val effectiveDate: LocalDate,

  val expiryDate: LocalDate? = null,

  @Column(updatable = false)
  val createdTime: LocalDateTime,

  @Column(updatable = false)
  val createdBy: String,

  val updatedTime: LocalDateTime? = null,

  val updatedBy: String? = null,
)
