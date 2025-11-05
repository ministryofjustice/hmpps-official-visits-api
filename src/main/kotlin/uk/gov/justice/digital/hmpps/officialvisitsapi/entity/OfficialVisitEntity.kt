package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
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
  @JoinColumn(name = "prison_visit_slot_id")
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

  @OneToMany(mappedBy = "officialVisit", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val officialVisitors: MutableList<OfficialVisitorEntity> = mutableListOf()

  fun addVisitor(
    visitorTypeCode: String,
    contactTypeCode: String,
    contactId: Long? = null,
    prisonerContactId: Long? = null,
    createdBy: User,
  ) {
    officialVisitors.add(
      OfficialVisitorEntity(
        officialVisit = this,
        visitorTypeCode = visitorTypeCode,
        contactTypeCode = contactTypeCode,
        contactId = contactId,
        prisonerContactId = prisonerContactId,
        createdBy = createdBy.username,
      ),
    )
  }

  fun officialVisitors(): List<OfficialVisitorEntity> = officialVisitors.toList()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as OfficialVisitEntity

    return officialVisitId == other.officialVisitId
  }

  override fun hashCode(): Int = officialVisitId.hashCode()
}
