package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import java.time.LocalDate

@Service
class VisitReviewService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val checker: VisitReviewChecker,
  private val releaseChecker: VisitReviewReleaseChecker,
  private val transferChecker: VisitReviewTransferChecker,
) {
  @Transactional
  fun check(officialVisitId: Long, checkType: VisitReviewCheckType) {
    val officialVisit = officialVisitRepository.findById(officialVisitId)
      .orElseThrow { throw EntityNotFoundException("Official visit with id $officialVisitId not found") }

    val today = LocalDate.now()

    when {
      officialVisit.visitStatusCode != VisitStatusType.SCHEDULED -> return
      officialVisit.visitDate < today -> return
      officialVisit.visitDate > today.plusDays(7) -> return
    }

    when (checkType) {
      VisitReviewCheckType.TRANSFER -> transferChecker.check(officialVisit)
      VisitReviewCheckType.RELEASE -> releaseChecker.check(officialVisit)
      else -> checker.check(officialVisit)
    }
  }
}

public enum class VisitReviewCheckType {
  CHECK,
  RECHECK,
  UPDATE,
  RELEASE,
  TRANSFER,
}
