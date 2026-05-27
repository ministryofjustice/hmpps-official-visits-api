package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.SentEmailRecordViewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.SentEmailSearchCriteria
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.SentEmailRecord
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.SentEmailRecordViewRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.SentEmailSearchInfo
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class SentEmailsService(
  private val sentEmailRecordViewRepository: SentEmailRecordViewRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val metricsService: MetricsService,
) {

  fun searchSentEmails(prisonCode: String, criteria: SentEmailSearchCriteria, page: Int, size: Int, user: User): PagedModel<SentEmailRecord> {
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
        sentEmailRecordViewRepository.findByPrisonCodeAndSentDateTimeGreaterThanEqualAndSentDateTimeLessThanOrderBySentDateTimeDesc(
          normalizedPrisonCode,
          fromDateTime,
          toDateTimeExclusive,
          pageable,
        )

      fromDateTime != null ->
        sentEmailRecordViewRepository.findByPrisonCodeAndSentDateTimeGreaterThanEqualOrderBySentDateTimeDesc(
          normalizedPrisonCode,
          fromDateTime,
          pageable,
        )

      toDateTimeExclusive != null ->
        sentEmailRecordViewRepository.findByPrisonCodeAndSentDateTimeLessThanOrderBySentDateTimeDesc(
          normalizedPrisonCode,
          toDateTimeExclusive,
          pageable,
        )

      else -> sentEmailRecordViewRepository.findByPrisonCodeOrderBySentDateTimeDesc(normalizedPrisonCode, pageable)
    }

    metricsService.send(
      eventType = MetricsEvents.SENT_EMAIL_SEARCH,
      info = SentEmailSearchInfo(
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
        val prisonerName = prisoner?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"

        viewEntity.toSentEmailRecord(prisonerName = prisonerName)
      },
    )
  }

  private fun SentEmailRecordViewEntity.toSentEmailRecord(prisonerName: String): SentEmailRecord {
    val emailType = notificationType.toEmailTypeOrNull()
    val normalizedNotificationType = emailType?.toApiNotificationType() ?: notificationType

    return SentEmailRecord(
      officialVisitId = officialVisitId,
      sentDate = sentDateTime.toLocalDate().format(dateFormatter),
      sentDateTime = sentDateTime.format(dateTimeFormatter),
      visitDate = visitDate.format(dateFormatter),
      visitStartTime = visitStartTime.format(timeFormatter),
      visitEndTime = visitEndTime.format(timeFormatter),
      prisonerName = prisonerName,
      prisonerNumber = prisonerNumber,
      emailAddress = emailAddress,
      emailStatus = emailStatus.name,
      notificationType = normalizedNotificationType,
      notificationTypeDescription = when (emailType) {
        EmailType.OFFICIAL_VISIT_CREATED -> "Visit Created"
        else -> "Unknown"
      },
    )
  }

  private fun String.toEmailTypeOrNull(): EmailType? = runCatching { EmailType.valueOf(this) }.getOrNull()

  private fun EmailType.toApiNotificationType(): String = when (this) {
    EmailType.OFFICIAL_VISIT_CREATED -> "CREATE"
  }

  private companion object {
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
  }
}
