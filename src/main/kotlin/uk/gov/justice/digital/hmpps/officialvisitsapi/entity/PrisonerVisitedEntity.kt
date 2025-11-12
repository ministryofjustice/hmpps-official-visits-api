package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.LocalDateTime.now

@Entity
@Table(name = "prisoner_visited")
data class PrisonerVisitedEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerVisitedId: Long = 0,

  @OneToOne
  @JoinColumn(name = "official_visit_id")
  val officialVisit: OfficialVisitEntity,

  val prisonerNumber: String,

  val attendanceCode: String? = null,

  val attendanceNotes: String? = null,

  val attendanceBy: String? = null,

  val attendanceTime: LocalDateTime? = null,

  val createdBy: String,

  val createdTime: LocalDateTime = now(),

  val updatedTime: LocalDateTime? = null,

  val updatedBy: String? = null,
)
