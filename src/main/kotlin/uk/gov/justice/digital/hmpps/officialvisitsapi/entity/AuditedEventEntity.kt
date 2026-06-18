package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
@Table(name = "audited_event")
open class AuditedEventEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val auditedEventId: Long = 0L,

  val officialVisitId: Long,

  val prisonCode: String,

  val prisonerNumber: String,

  val eventSource: String,

  val userName: String,

  val userFullName: String,

  val summaryText: String,

  val detailText: String,

  val eventDateTime: LocalDateTime = LocalDateTime.now(),

  private val versionNumber: Int? = null,
) {
  // Defaults to 1 if not set to handle older events. Version 2 is the latest at the time of writing.
  fun version() = versionNumber ?: 1

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as AuditedEventEntity

    return auditedEventId == other.auditedEventId
  }

  override fun hashCode(): Int = auditedEventId.hashCode()
}
