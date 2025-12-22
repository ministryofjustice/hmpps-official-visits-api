package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "v_official_visit_summary")
data class OfficialVisitSummaryEntity(
  @Id
  val officialVisitId: Long,

  val prisonCode: String,

  val prisonerNumber: String,

  @Enumerated(EnumType.STRING)
  val visitStatusCode: VisitStatusType,

  @Enumerated(EnumType.STRING)
  val visitTypeCode: VisitType,

  val visitDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val dpsLocationId: UUID,

  val prisonVisitSlotId: Long,

  val staffNotes: String?,

  val prisonerNotes: String?,

  val visitorConcernNotes: String?,

  @Enumerated(EnumType.STRING)
  val completionCode: VisitCompletionType?,

  val createdBy: String,

  val createdTime: LocalDateTime,

  val updatedBy: String?,

  val updatedTime: LocalDateTime?,

  val offenderBookId: Long?,

  val numberOfVisitors: Int,
)
