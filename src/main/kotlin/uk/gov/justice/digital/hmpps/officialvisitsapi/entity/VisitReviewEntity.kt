package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
@Table(name = "visit_review")
class VisitReviewEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val visitReviewId: Long = 0,

  val officialVisitId: Long,

  @OneToMany(mappedBy = "visitReview", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val visitReviewDetails: MutableList<VisitReviewDetailEntity> = mutableListOf(),

  val raisedTime: LocalDateTime,
) {
  var expiredTime: LocalDateTime? = null

  fun addVisitReviewDetails(
    raisedTime: LocalDateTime,
    issueType: IssueType,
    issueDetail: String?,
  ) {
    visitReviewDetails.add(
      VisitReviewDetailEntity(
        visitReview = this,
        raisedTime = raisedTime,
        issueType = issueType,
        issueDetail = issueDetail,
      ),
    )
  }

  fun expire() {
    if (expiredTime != null) {
      return
    }
    expiredTime = LocalDateTime.now()
  }

  fun visitReviewDetails() = visitReviewDetails.toList()

  fun updateAcknowledgedDetails(acknowledgedTime: LocalDateTime, acknowledgedBy: String) {
    if (expiredTime != null) {
      return
    }

    visitReviewDetails
      .filter { it.acknowledgedTime == null && it.acknowledgedBy == null }
      .forEach { detail ->
        detail.acknowledgedTime = acknowledgedTime
        detail.acknowledgedBy = acknowledgedBy
      }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as VisitReviewEntity

    return visitReviewId == other.visitReviewId
  }

  override fun hashCode(): Int = visitReviewId.hashCode()
}
