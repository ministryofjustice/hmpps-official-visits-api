package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitChangeStatusResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AuditedEventRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository

/**
 * Service to detect if visit details have changed since a notification was sent.
 */
@Service
class VisitChangeDetectionService(
  private val auditedEventRepository: AuditedEventRepository,
  private val notificationRepository: NotificationRepository,
) {

  /**
   * Checks if any monitored visit fields have changed since the last notification was sent, or whether the email was sent after the visit was created.
   *
   * @param officialVisitId The ID of the official visit to check
   * @return true if the visit details have changed since the last notification was sent, or if no notification has been sent; false otherwise.
   */
  fun requiresEmailUpdate(officialVisitId: Long): VisitChangeStatusResponse {
    val mostRecentNotification =
      notificationRepository.findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
        ?: return VisitChangeStatusResponse(hasChanged = true) // there is no notification, so should send the confirmation email

    val status = auditedEventRepository.findRelevantAuditEventsAfter(
      officialVisitId,
      mostRecentNotification.createdTime,
      PageRequest.of(0, 1),
    ).isNotEmpty()
    return VisitChangeStatusResponse(hasChanged = status)
  }
}
