package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Immutable
@Table(name = "v_sent_email_notifications")
data class SentNotificationEntity(
  @Id
  val notificationId: Long,

  val officialVisitId: Long,

  val prisonCode: String,

  val sentDateTime: LocalDateTime,

  val visitDate: LocalDate,

  val visitStartTime: LocalTime,

  val visitEndTime: LocalTime,

  val emailAddress: String,

  @Enumerated(EnumType.STRING)
  val emailStatus: NotificationEmailStatus,

  val notificationType: String,

  val prisonerNumber: String,
)
