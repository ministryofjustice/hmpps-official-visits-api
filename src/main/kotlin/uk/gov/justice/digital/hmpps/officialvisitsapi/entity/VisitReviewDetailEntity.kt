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
import java.time.LocalDateTime

@Entity
@Table(name = "visit_review_detail")
class VisitReviewDetailEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val visitReviewDetailId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "visit_review_id")
  val visitReview: VisitReviewEntity,

  val raisedTime: LocalDateTime,

  @Enumerated(EnumType.STRING)
  val issueType: IssueType,

  val issueDetail: String? = null,
) {
  var acknowledgedTime: LocalDateTime? = null

  var acknowledgedBy: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as VisitReviewDetailEntity

    return visitReviewDetailId == other.visitReviewDetailId
  }

  override fun hashCode(): Int = visitReviewDetailId.hashCode()
}

enum class IssueType {
  PRISONER_NEW_ALERT,
  PRISONER_NEW_RESTRICTION,
  PRISONER_RELEASED,
  PRISONER_TRANSFERRED,
  VISITOR_NO_RELATIONSHIP,
  VISITOR_NOT_APPROVED,
  VISITOR_NOT_OFFICIAL,
}
