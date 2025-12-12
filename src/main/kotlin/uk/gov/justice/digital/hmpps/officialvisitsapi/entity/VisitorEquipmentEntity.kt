package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
@Table(name = "visitor_equipment")
class VisitorEquipmentEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val visitorEquipmentId: Long = 0,

  @OneToOne
  @JoinColumn(name = "official_visitor_id")
  val officialVisitor: OfficialVisitorEntity,

  var description: String,

  val createdBy: String,
) {
  val createdTime: LocalDateTime = LocalDateTime.now()

  var approved: Boolean = false

  var approvedTime: LocalDateTime? = null

  var approvedBy: String? = null

  var updatedBy: String? = null

  var updatedTime: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as VisitorEquipmentEntity

    return visitorEquipmentId == other.visitorEquipmentId
  }

  override fun hashCode(): Int = visitorEquipmentId.hashCode()
}
