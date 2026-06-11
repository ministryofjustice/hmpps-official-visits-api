package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.SentNotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationSearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.SentNotification
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationSearchRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.NotificationSearchInfo
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class SentEmailsService(
  private val notificationSearchRepository: NotificationSearchRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val metricsService: MetricsService,
) {

  fun searchSentEmails(prisonCode: String, criteria: NotificationSearchRequest, page: Int, size: Int, user: User): PagedModel<SentNotification> {
    require(page >= 0) { "Page number must be greater than or equal to zero" }
    require(size > 0) { "Page size must be greater than zero" }
    val normalizedPrisonCode = prisonCode.trim()
    require(normalizedPrisonCode.isNotEmpty()) { "Prison code must be provided" }
    require(criteria.fromDate == null || criteria.toDate == null || !criteria.fromDate.isAfter(criteria.toDate)) {
      "From date must be on or before to date"
    }

    val fromDateTime = criteria.fromDate?.atStartOfDay()
    val toDateTimeExclusive = criteria.toDate?.plusDays(1)?.atStartOfDay()

    val pageable = PageRequest.of(page, size)
    val pageResult = when {
      fromDateTime != null && toDateTimeExclusive != null ->
        notificationSearchRepository.findByPrisonCodeAndSentDateTimeGreaterThanEqualAndSentDateTimeLessThanOrderBySentDateTimeDesc(
          normalizedPrisonCode,
          fromDateTime,
          toDateTimeExclusive,
          pageable,
        )

      fromDateTime != null ->
        notificationSearchRepository.findByPrisonCodeAndSentDateTimeGreaterThanEqualOrderBySentDateTimeDesc(
          normalizedPrisonCode,
          fromDateTime,
          pageable,
        )

      toDateTimeExclusive != null ->
        notificationSearchRepository.findByPrisonCodeAndSentDateTimeLessThanOrderBySentDateTimeDesc(
          normalizedPrisonCode,
          toDateTimeExclusive,
          pageable,
        )

      else -> notificationSearchRepository.findByPrisonCodeOrderBySentDateTimeDesc(normalizedPrisonCode, pageable)
    }

    metricsService.send(
      eventType = MetricsEvents.NOTIFICATION_SEARCH,
      info = NotificationSearchInfo(
        prisonCode = normalizedPrisonCode,
        username = user.username,
        fromDate = criteria.fromDate,
        toDate = criteria.toDate,
        numberOfResults = pageResult.numberOfElements,
      ),
    )

    val prisonerNumbers = pageResult.content.map { it.prisonerNumber }.distinct()

    val prisonerMap = if (prisonerNumbers.isEmpty()) {
      emptyMap()
    } else {
      prisonerSearchClient.findByPrisonerNumbers(
        prisonerNumbers,
        prisonerNumbers.size,
      ).associateBy { it.prisonerNumber }
    }

    return PagedModel(
      pageResult.map { viewEntity ->
        val prisoner = prisonerMap[viewEntity.prisonerNumber]
        viewEntity.toSentEmailRecord(prisoner)
      },
    )
  }

  private fun SentNotificationEntity.toSentEmailRecord(prisoner: Prisoner?): SentNotification {
    val emailType = notificationType.toEmailTypeOrNull()
    val normalizedNotificationType = emailType?.toApiNotificationType() ?: notificationType

    return SentNotification(
      officialVisitId = officialVisitId,
      sentDate = sentDateTime.toLocalDate().format(dateFormatter),
      sentDateTime = sentDateTime.format(dateTimeFormatter),
      visitDate = visitDate.format(dateFormatter),
      visitStartTime = visitStartTime.format(timeFormatter),
      visitEndTime = visitEndTime.format(timeFormatter),
      firstName = prisoner?.firstName,
      lastName = prisoner?.lastName,
      prisonerNumber = prisonerNumber,
      emailAddress = emailAddress,
      emailStatus = emailStatus.name,
      notificationType = normalizedNotificationType,
      notificationTypeDescription = when (emailType) {
        EmailType.OFFICIAL_VISIT_CREATED -> "Visit Created"
        EmailType.OFFICIAL_VISIT_UPDATED -> "Visit Updated"
        EmailType.OFFICIAL_VISIT_CANCELLED -> "Visit Cancelled"
        else -> "Unknown"
      },
    )
  }

  private fun String.toEmailTypeOrNull(): EmailType? = runCatching { EmailType.valueOf(this) }.getOrNull()

  private fun EmailType.toApiNotificationType(): String = when (this) {
    EmailType.OFFICIAL_VISIT_CREATED -> "CREATE"
    EmailType.OFFICIAL_VISIT_UPDATED -> "UPDATED"
    EmailType.OFFICIAL_VISIT_CANCELLED -> "CANCELLED"
  }

  private companion object {
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
  }
}
