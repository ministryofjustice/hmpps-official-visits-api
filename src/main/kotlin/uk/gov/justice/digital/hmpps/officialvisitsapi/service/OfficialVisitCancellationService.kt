package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.time.LocalDateTime

@Service
@Transactional
class OfficialVisitCancellationService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }
  fun cancelOfficialVisit(
    prisonCode: String,
    officialVisitId: Long,
    request: OfficialVisitCancellationRequest,
    user: User,
  ) {
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

    logger.info("Official visit with ID $officialVisitId cancelled.")
  }
}
