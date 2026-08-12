package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "v_visits_for_review")
data class VisitForReviewEntity(
  @Id
  val officialVisitId: Long,

  val prisonCode: String,

  val prisonerNumber: String,

  val visitDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val dpsLocationId: UUID,

  @Enumerated(EnumType.STRING)
  val visitStatusCode: VisitStatusType,

  @Enumerated(EnumType.STRING)
  val visitTypeCode: VisitType,

  val visitReviewId: Long,

  val raisedTime: LocalDateTime,

  val expiredTime: LocalDateTime? = null,

  val visitReviewDetailId: Long,

  @Enumerated(EnumType.STRING)
  val issueType: IssueType,

  val issueDetail: String? = null,

  val acknowledgedTime: LocalDateTime? = null,

  val acknowledgedBy: String? = null,

  val detailRaisedTime: LocalDateTime,
)
