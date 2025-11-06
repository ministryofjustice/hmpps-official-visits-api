package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
@Table(name = "official_visitor")
class OfficialVisitorEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val officialVisitorId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "official_visit_id")
  val officialVisit: OfficialVisitEntity,

  val visitorTypeCode: String,

  val contactTypeCode: String,

  val contactId: Long? = null,

  val prisonerContactId: Long? = null,

  val leadVisitor: Boolean = false,

  val assistedVisit: Boolean = false,

  val createdBy: String,
) {
  val createdTime: LocalDateTime = LocalDateTime.now()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as OfficialVisitorEntity

    return officialVisitorId == other.officialVisitorId
  }

  override fun hashCode(): Int = officialVisitorId.hashCode()
}
