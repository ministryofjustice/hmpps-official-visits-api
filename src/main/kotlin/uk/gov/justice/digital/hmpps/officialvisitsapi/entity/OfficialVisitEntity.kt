package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "official_visit")
class OfficialVisitEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val officialVisitId: Long = 0,

  @OneToOne
  @JoinColumn(name = "prison_visit_slot_id")
  val prisonVisitSlot: PrisonVisitSlotEntity,

  val visitDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val dpsLocationId: UUID,

  @Enumerated(EnumType.STRING)
  val visitStatusCode: VisitStatusType,

  @Enumerated(EnumType.STRING)
  val visitTypeCode: VisitType,

  val prisonCode: String,

  val prisonerNumber: String,

  val currentTerm: Boolean = true,

  val staffNotes: String? = null,

  val prisonerNotes: String? = null,

  val visitorConcernNotes: String? = null,

  @Enumerated(EnumType.STRING)
  val searchTypeCode: SearchLevelType? = null,

  @Enumerated(EnumType.STRING)
  val completionCode: VisitCompletionType? = null,

  val overrideBanTime: LocalDateTime? = null,

  val overrideBanBy: String? = null,

  val createdBy: String,

  val createdTime: LocalDateTime = now(),

  val updatedBy: String? = null,

  val updatedTime: LocalDateTime? = null,

  val offenderBookId: Long? = null,

  val offenderVisitId: Long? = null,

  val visitOrderNumber: Long? = null,
) {
  @OneToMany(mappedBy = "officialVisit", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val officialVisitors: MutableList<OfficialVisitorEntity> = mutableListOf()

  fun addVisitor(
    visitorTypeCode: VisitorType,
    relationshipTypeCode: RelationshipType,
    relationshipCode: String,
    contactId: Long? = null,
    prisonerContactId: Long? = null,
    firstName: String? = null,
    lastName: String? = null,
    leadVisitor: Boolean = false,
    assistedVisit: Boolean = false,
    assistedNotes: String? = null,
    createdBy: User,
    createdTime: LocalDateTime = now(),
  ): OfficialVisitorEntity = run {
    OfficialVisitorEntity(
      officialVisit = this,
      visitorTypeCode = visitorTypeCode,
      relationshipTypeCode = relationshipTypeCode,
      relationshipCode = relationshipCode,
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      firstName = firstName,
      lastName = lastName,
      leadVisitor = leadVisitor,
      assistedVisit = assistedVisit,
      visitorNotes = assistedNotes.takeIf { assistedVisit }, // assisted notes are only applicable when assistedVisit == true
      createdBy = createdBy.username,
      createdTime = createdTime,
    ).also(officialVisitors::add)
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
