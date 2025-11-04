package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now

@Entity
@Table(name = "official_visit")
class OfficialVisitEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val officialVisitId: Long = 0,

  @OneToOne
  @JoinColumn(name = "prison_visit_slot_id", nullable = false)
  val prisonVisitSlot: PrisonVisitSlotEntity,

  val prisonCode: String,

  val prisonerNumber: String,

  val visitDate: LocalDate,

  val visitStatusCode: String,

  val visitTypeCode: String,

  val createdBy: String,
) {
  val createdTime: LocalDateTime = now()

  var updatedBy: String? = null

  var updatedTime: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as OfficialVisitEntity

    return officialVisitId == other.officialVisitId
  }

  override fun hashCode(): Int = officialVisitId.hashCode()
}
