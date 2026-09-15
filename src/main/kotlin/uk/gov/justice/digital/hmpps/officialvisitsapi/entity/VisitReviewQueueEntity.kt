package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import java.time.LocalDateTime

@Entity
@Table(name = "visit_review_queue")
class VisitReviewQueueEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val visitReviewQueueId: Long = 0,

  val officialVisitId: Long,

  val createdTime: LocalDateTime,

  @Enumerated(EnumType.STRING)
  val triggeringEvent: VisitReviewCheckType = VisitReviewCheckType.CHECK,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as VisitReviewQueueEntity

    return visitReviewQueueId == other.visitReviewQueueId
  }

  override fun hashCode(): Int = visitReviewQueueId.hashCode()
}
