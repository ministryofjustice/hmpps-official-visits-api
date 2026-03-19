package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitCancellationEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.VisitMetricInfo
import java.time.LocalDateTime

@Service
@Transactional
class OfficialVisitCancellationService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val auditingService: AuditingService,
  private val metricsService: MetricsService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }
  fun cancelOfficialVisit(
    prisonCode: String,
    officialVisitId: Long,
    request: OfficialVisitCancellationRequest,
    user: User,
  ): OfficialVisitCancelledDto {
    val officialVisit = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(officialVisitId, prisonCode)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId and prison code $prisonCode not found")
    val prisonerVisited = prisonerVisitedRepository.findByOfficialVisit(officialVisit)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId prisoner not found")

    officialVisit.cancel(request.cancellationReason!!, request.cancellationNotes, user)

    prisonerVisitedRepository.saveAndFlush(
      prisonerVisited.copy(
        attendanceCode = AttendanceType.ABSENT,
        updatedBy = user.username,
        updatedTime = LocalDateTime.now(),
      ),
    )
    metricsService.send(
      MetricsEvents.CANCEL,
      VisitMetricInfo(
        username = user.username,
        officialVisitId = officialVisit.officialVisitId,
        prisonCode = officialVisit.prisonCode,
        prisonerNumber = officialVisit.prisonerNumber,
        numberOfVisitors = officialVisit.officialVisitors().size.toLong(),
        startTime = officialVisit.startTime,
      ),
    )

    auditingService.recordAuditEvent(
      auditVisitCancellationEvent {
        officialVisitId(officialVisit.officialVisitId)
        summaryText("Official visit cancelled")
        eventSource("DPS")
        user(user)
        prisonCode(prisonCode)
        prisonerNumber(prisonerVisited.prisonerNumber)
      },
    )

    return OfficialVisitCancelledDto(
      prisonCode = prisonCode,
      officialVisitId = officialVisitId,
      prisonerVisitedId = prisonerVisited.prisonerVisitedId,
      prisonerNumber = prisonerVisited.prisonerNumber,
      visitorAndContactIds = officialVisit.officialVisitors().map { it.officialVisitorId to it.contactId },
    ).also {
      logger.info("Official visit with ID $officialVisitId cancelled.")
    }
  }

  data class OfficialVisitCancelledDto(
    val prisonCode: String,
    val officialVisitId: Long,
    val prisonerVisitedId: Long,
    val prisonerNumber: String,
    val visitorAndContactIds: List<Pair<Long, Long?>>,
  )
}
