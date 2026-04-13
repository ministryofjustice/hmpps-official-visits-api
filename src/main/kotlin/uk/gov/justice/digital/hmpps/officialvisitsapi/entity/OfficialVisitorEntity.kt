package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.OfficialVisitorAuditEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(OfficialVisitorAuditEntityListener::class)
@Table(name = "official_visitor")
class OfficialVisitorEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val officialVisitorId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "official_visit_id")
  val officialVisit: OfficialVisitEntity,

  @Enumerated(EnumType.STRING)
  var visitorTypeCode: VisitorType,

  var firstName: String? = null,

  var lastName: String? = null,

  var contactId: Long? = null,

  var prisonerContactId: Long? = null,

  @Enumerated(EnumType.STRING)
  var relationshipTypeCode: RelationshipType? = null,

  var relationshipCode: String? = null,

  var leadVisitor: Boolean = false,

  var assistedVisit: Boolean = false,

  var visitorNotes: String? = null,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),

  var offenderVisitVisitorId: Long? = null,
) {
  @OneToOne(mappedBy = "officialVisitor", cascade = [CascadeType.ALL], orphanRemoval = true)
  var visitorEquipment: VisitorEquipmentEntity? = null

  @Enumerated(EnumType.STRING)
  var attendanceCode: AttendanceType? = null

  var updatedBy: String? = null

  var updatedTime: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as OfficialVisitorEntity

    return officialVisitorId == other.officialVisitorId
  }

  override fun hashCode(): Int = officialVisitorId.hashCode()
}
