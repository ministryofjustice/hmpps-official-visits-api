package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "notification")
class NotificationEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val notificationId: Long = 0,

  val officialVisitId: Long,

  val templateId: String,

  val emailAddress: String,

  val reason: String,

  val govNotifyNotificationId: UUID,

  @Enumerated(EnumType.STRING)
  val emailStatus: NotificationEmailStatus = NotificationEmailStatus.PENDING,

  val createdTime: LocalDateTime = LocalDateTime.now(),

  var status: String? = null,

  var statusUpdatedTime: LocalDateTime? = null,
)

enum class NotificationEmailStatus {
  PENDING,
  SENT,
  FAILED,
}
