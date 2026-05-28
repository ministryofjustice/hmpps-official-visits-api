package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotifyCallbackRequest
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
  fun processCallback(request: NotifyCallbackRequest, authorizationHeader: String?) {
    if (callbackBearerToken.isNotBlank() && authorizationHeader != "Bearer $callbackBearerToken") {
      throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid callback token")
    }

    val notification = notificationRepository.findByGovNotifyNotificationId(request.id)

    if (notification == null) {
      logger.warn("Received GOV.UK Notify callback for unknown notification id {}", request.id)
      return
    }

    notification.status = request.status
    notification.statusUpdatedTime = request.completedAt?.toLocalDateTime() ?: LocalDateTime.now()

    logger.info(
      "Processed GOV.UK Notify callback for notification id {} with status {}",
      request.id,
      request.status,
    )
  }
}
