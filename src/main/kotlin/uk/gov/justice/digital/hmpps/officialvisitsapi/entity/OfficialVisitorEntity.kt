package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
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

  @Enumerated(EnumType.STRING)
  val visitorTypeCode: VisitorType,

  val firstName: String? = null,

  val lastName: String? = null,

  val contactId: Long? = null,

  val prisonerContactId: Long? = null,

  @Enumerated(EnumType.STRING)
  val relationshipTypeCode: RelationshipType? = null,

  val relationshipCode: String? = null,

  val leadVisitor: Boolean = false,

  val assistedVisit: Boolean = false,

  val visitorNotes: String? = null,

  @Enumerated(EnumType.STRING)
  val attendanceCode: AttendanceType? = null,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),

  val updatedBy: String? = null,

  val updatedTime: LocalDateTime? = null,

  val offenderVisitVisitorId: Long? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as OfficialVisitorEntity

    return officialVisitorId == other.officialVisitorId
  }

  override fun hashCode(): Int = officialVisitorId.hashCode()
}
