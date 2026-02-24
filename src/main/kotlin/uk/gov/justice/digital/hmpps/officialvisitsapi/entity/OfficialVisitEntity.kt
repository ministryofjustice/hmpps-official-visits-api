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
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.requireNot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
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
  var prisonVisitSlot: PrisonVisitSlotEntity,

  var visitDate: LocalDate,

  var startTime: LocalTime,

  var endTime: LocalTime,

  var dpsLocationId: UUID,

  @Enumerated(EnumType.STRING)
  var visitTypeCode: VisitType,

  var prisonCode: String,

  var prisonerNumber: String,

  var currentTerm: Boolean = true,

  var staffNotes: String? = null,

  var prisonerNotes: String? = null,

  var visitorConcernNotes: String? = null,

  val overrideBanTime: LocalDateTime? = null,

  var overrideBanBy: String? = null,

  val createdBy: String,

  val createdTime: LocalDateTime = now(),

  var offenderBookId: Long? = null,

  var offenderVisitId: Long? = null,

  var visitOrderNumber: Long? = null,
) {
  @OneToMany(mappedBy = "officialVisit", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val officialVisitors: MutableList<OfficialVisitorEntity> = mutableListOf()

  @Enumerated(EnumType.STRING)
  var visitStatusCode: VisitStatusType = VisitStatusType.SCHEDULED
    private set

  @Enumerated(EnumType.STRING)
  var completionCode: VisitCompletionType? = null
    private set

  var completionNotes: String? = null

  @Enumerated(EnumType.STRING)
  var searchTypeCode: SearchLevelType? = null
    private set

  var updatedBy: String? = null
    private set

  var updatedTime: LocalDateTime? = null
    private set

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
      visitorNotes = assistedNotes,
      createdBy = createdBy.username,
      createdTime = createdTime,
    ).also(officialVisitors::add)
  }

  fun removeVisitor(officialVisitorEntity: OfficialVisitorEntity) {
    officialVisitors.remove(officialVisitorEntity)
  }

  fun officialVisitors(): List<OfficialVisitorEntity> = officialVisitors.toList()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as OfficialVisitEntity

    return officialVisitId == other.officialVisitId
  }

  override fun hashCode(): Int = officialVisitId.hashCode()

  fun complete(
    completionCode: VisitCompletionType,
    prisonerSearchType: SearchLevelType,
    visitorAttendance: Map<Long, AttendanceType>,
    completedBy: User,
    completionNotes: String?,
  ) = apply {
    require(this.visitStatusCode in listOf(VisitStatusType.SCHEDULED, VisitStatusType.EXPIRED)) {
      "Only scheduled or expired visits can be completed."
    }

    requireNot(completionCode.isCancellation) {
      "$completionCode is not a valid completion code"
    }

    val timestamp = now()

    visitorAttendance.forEach { attendance ->
      officialVisitors.single { ov -> ov.officialVisitorId == attendance.key }.apply {
        this.attendanceCode = attendance.value
        this.updatedBy = completedBy.username
        this.updatedTime = timestamp
      }
    }

    this.visitStatusCode = VisitStatusType.COMPLETED
    this.completionCode = completionCode
    this.completionNotes = completionNotes
    this.searchTypeCode = prisonerSearchType
    this.updatedBy = completedBy.username
    this.updatedTime = timestamp
  }

  fun cancel(
    cancellationCode: VisitCompletionType,
    cancellationNotes: String?,
    cancelledBy: User,
  ) = apply {
    require(this.visitStatusCode in listOf(VisitStatusType.SCHEDULED, VisitStatusType.EXPIRED)) {
      "Only scheduled or expired visits can be cancelled."
    }

    require(cancellationCode.isCancellation) {
      "$cancellationCode is not a valid cancellation code"
    }

    val timestamp = now()

    officialVisitors.forEach { visitor ->
      visitor.apply {
        attendanceCode = AttendanceType.ABSENT
        updatedBy = cancelledBy.username
        updatedTime = timestamp
      }
    }

    this.visitStatusCode = VisitStatusType.CANCELLED
    this.completionCode = cancellationCode
    this.completionNotes = cancellationNotes

    this.updatedBy = cancelledBy.username
    this.updatedTime = timestamp
  }

  fun update(
    prisonVisitSlot: PrisonVisitSlotEntity,
    visitDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    dpsLocationId: UUID,
    visitTypeCode: VisitType,
    prisonCode: String,
    prisonerNumber: String,
    currentTerm: Boolean,
    staffNotes: String?,
    prisonerNotes: String?,
    visitorConcernNotes: String?,
    overrideBanBy: String?,
    offenderBookId: Long?,
    offenderVisitId: Long?,
    visitOrderNumber: Long?,
    visitStatusCode: VisitStatusType,
    completionCode: VisitCompletionType?,
    completionNotes: String?,
    prisonerSearchType: SearchLevelType?,
    updatedBy: String,
    updatedTime: LocalDateTime,
  ) = apply {
    this.prisonVisitSlot = prisonVisitSlot
    this.visitDate = visitDate
    this.startTime = startTime
    this.endTime = endTime
    this.dpsLocationId = dpsLocationId
    this.visitTypeCode = visitTypeCode
    this.prisonCode = prisonCode
    this.prisonerNumber = prisonerNumber
    this.currentTerm = currentTerm
    this.staffNotes = staffNotes
    this.prisonerNotes = prisonerNotes
    this.visitorConcernNotes = visitorConcernNotes
    this.overrideBanBy = overrideBanBy
    this.offenderBookId = offenderBookId
    this.offenderVisitId = offenderVisitId
    this.visitOrderNumber = visitOrderNumber
    this.visitStatusCode = visitStatusCode
    this.completionCode = completionCode
    this.completionNotes = completionNotes
    this.searchTypeCode = prisonerSearchType
    this.updatedBy = updatedBy
    this.updatedTime = updatedTime
  }

  companion object {
    fun migrated(visitSlot: PrisonVisitSlotEntity, request: MigrateVisitRequest) = run {
      OfficialVisitEntity(
        prisonVisitSlot = visitSlot,
        visitDate = request.visitDate!!,
        startTime = request.startTime!!,
        endTime = request.endTime!!,
        dpsLocationId = request.dpsLocationId!!,
        visitTypeCode = request.visitTypeCode!!,
        prisonCode = request.prisonCode!!,
        prisonerNumber = request.prisonerNumber!!,
        currentTerm = request.currentTerm!!,
        staffNotes = null, // Never supplied
        prisonerNotes = request.commentText,
        visitorConcernNotes = request.visitorConcernText,
        overrideBanTime = null, // Never supplied
        overrideBanBy = request.overrideBanStaffUsername,
        createdBy = request.createUsername ?: "MIGRATION",
        createdTime = request.createDateTime ?: LocalDateTime.now(),
        offenderBookId = request.offenderBookId,
        offenderVisitId = request.offenderVisitId!!,
        visitOrderNumber = request.visitOrderNumber,
      ).apply {
        this.visitStatusCode = request.visitStatusCode!!
        this.completionCode = request.visitCompletionCode
        this.searchTypeCode = request.searchTypeCode
        this.updatedBy = request.modifyUsername
        this.updatedTime = request.modifyDateTime
      }
    }

    fun synchronised(visitSlot: PrisonVisitSlotEntity, request: SyncCreateOfficialVisitRequest) = run {
      OfficialVisitEntity(
        prisonVisitSlot = visitSlot,
        visitDate = request.visitDate!!,
        startTime = request.startTime!!,
        endTime = request.endTime!!,
        dpsLocationId = request.dpsLocationId!!,
        visitTypeCode = request.visitTypeCode!!,
        prisonCode = request.prisonCode!!,
        prisonerNumber = request.prisonerNumber!!,
        currentTerm = request.currentTerm!!,
        staffNotes = null, // Never supplied from NOMIS
        prisonerNotes = request.commentText,
        visitorConcernNotes = request.visitorConcernText,
        overrideBanTime = null, // Never supplied from NOMIS
        overrideBanBy = request.overrideBanStaffUsername,
        createdBy = request.createUsername ?: "SYNC",
        createdTime = request.createDateTime ?: now(),
        offenderBookId = request.offenderBookId,
        offenderVisitId = request.offenderVisitId!!, // The visit id in NOMIS - reference only
        visitOrderNumber = request.visitOrderNumber, // Not usually supplied - reference only
      ).apply {
        this.visitStatusCode = request.visitStatusCode!!
        this.completionCode = request.visitCompletionCode
        this.searchTypeCode = request.searchTypeCode
      }
    }
  }
}
