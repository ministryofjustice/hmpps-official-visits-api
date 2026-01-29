package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCompletionRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository

@Service
@Transactional
class OfficialVisitCompletionService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun completeOfficialVisit(
    prisonCode: String,
    officialVisitId: Long,
    request: OfficialVisitCompletionRequest,
    user: User,
  ) {
    val officialVisit = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(officialVisitId, prisonCode)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId and prison code $prisonCode not found")
    val prisonerVisited = prisonerVisitedRepository.findByOfficialVisit(officialVisit)
      ?: throw EntityNotFoundException("Official visit with id $officialVisitId prisoner not found")

    officialVisitRepository.saveAndFlush(
      officialVisit.complete(
        completionCode = request.completionReason!!,
        prisonerSearchType = request.prisonerSearchType!!,
        visitorAttendance = request.visitorAttendance.associateBy(
          { it.officialVisitorId },
          { it.visitorAttendance!! },
        ),
        user,
      ),
    )

    prisonerVisitedRepository.saveAndFlush(prisonerVisited.copy(attendanceCode = request.prisonerAttendance))

    logger.info("Official visit with ID $officialVisitId completed.")
  }
}
