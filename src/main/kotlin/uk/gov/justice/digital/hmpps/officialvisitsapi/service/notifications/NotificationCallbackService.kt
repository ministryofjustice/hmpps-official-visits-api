package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotifyCallbackNotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class NotificationCallbackService(
  private val notificationRepository: NotificationRepository,
  @Value("\${notify.callback.secret:null}") private val notifySecret: String?,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun processCallback(request: NotifyCallbackNotificationRequest, providedSecret: String?) {
    if (providedSecret.isNullOrEmpty() || notifySecret != providedSecret.removePrefix("Bearer ")) {
      throw BadCredentialsException("Callback secret does not match the supplied secret")
    }

    val notification = notificationRepository.findByGovNotifyNotificationId(request.notificationId)
    if (notification == null) {
      logger.warn("Received GOV.UK Notify callback for unknown notification id {} and completed time {}", request.notificationId, request.completedAt)
      return
    }

    notification.emailStatus = request.status.toEmailStatus()
    notification.statusUpdatedTime =
      request.completedAt
        ?.atZoneSameInstant(ZoneId.systemDefault())
        ?.toLocalDateTime()
        ?: LocalDateTime.now()

    logger.info(
      "Processed GOV.UK Notify callback for notification id {} with status {}",
      request.notificationId,
      request.status,
    )
  }

  private fun String.toEmailStatus(): NotificationEmailStatus = when (this.lowercase()) {
    "delivered" -> NotificationEmailStatus.SENT
    "permanent-failure" -> NotificationEmailStatus.PERMANENT_FAILURE
    "temporary-failure" -> NotificationEmailStatus.TEMPORARY_FAILURE
    "technical-failure" -> NotificationEmailStatus.TECHNICAL_FAILURE
    else -> {
      logger.warn("Unknown GOV.UK Notify callback status received: {}", this)
      NotificationEmailStatus.UNKNOWN
    }
  }
}
