package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotifyCallbackNotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import java.time.LocalDateTime

@Component
class NotifyCallbackService(
  private val notificationRepository: NotificationRepository,
  @Value("\${notify.callback.bearer-token:}") private val callbackBearerToken: String,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun processCallback(request: NotifyCallbackNotificationRequest, authorizationHeader: String?) {
    if (callbackBearerToken.isNotBlank() && authorizationHeader != "Bearer $callbackBearerToken") {
      throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid callback token")
    }

    val notification = notificationRepository.findByGovNotifyNotificationId(request.notificationId)

    if (notification == null) {
      logger.warn("Received GOV.UK Notify callback for unknown notification id {}", request.notificationId)
      return
    }

    notification.emailStatus = request.status.toEmailStatus()
    notification.statusUpdatedTime = request.completedAt ?: LocalDateTime.now()

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
