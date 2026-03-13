package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Id
import org.hibernate.Hibernate
import java.time.LocalDateTime

// TODO and missing entity annotation
class AuditedEventEntity(
  @Id
  val auditedEventId: Long = 0L,

  val officialVisitId: Long,

  val prisonCode: String,

  val prisonDescription: String,

  val prisonerNumber: String,

  val eventSource: String,

  val username: String,

  val userFullName: String,

  val summaryText: String,

  val detailText: String,

  val eventDateTime: LocalDateTime = LocalDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as AuditedEventEntity

    return auditedEventId == other.auditedEventId
  }

  override fun hashCode(): Int = auditedEventId.hashCode()
}
