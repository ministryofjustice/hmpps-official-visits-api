package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "prison_visit_slot")
data class PrisonVisitSlotEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonVisitSlotId: Long = 0,

  val prisonTimeSlotId: Long = 0,

  val dpsLocationId: UUID,

  val maxAdults: Int? = null,

  val maxGroups: Int? = null,

  val maxVideoSessions: Int? = null,

  @Column(updatable = false)
  val createdTime: LocalDateTime,

  @Column(updatable = false)
  val createdBy: String,

  val updatedTime: LocalDateTime? = null,

  val updatedBy: String? = null,
)
