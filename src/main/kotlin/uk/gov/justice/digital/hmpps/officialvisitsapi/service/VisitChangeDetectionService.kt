package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
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
   * Checks if any monitored visit fields have changed since the last notification was sent.
   *
   * @param officialVisitId The ID of the official visit to check
   * @return true when a relevant change was recorded after the latest notification, otherwise false
   */
  fun hasVisitDetailsChanged(officialVisitId: Long): Boolean {
    val mostRecentNotification =
      notificationRepository.findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
        ?: return false

    return auditedEventRepository.findRelevantAuditEventsAfter(
      officialVisitId,
      mostRecentNotification.createdTime,
      PageRequest.of(0, 1),
    ).isNotEmpty()
  }
}
