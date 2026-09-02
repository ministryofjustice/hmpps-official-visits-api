package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

@Service
class VisitReviewService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val checker: VisitReviewChecker,
  private val releaseChecker: VisitReviewReleaseChecker,
  private val transferChecker: VisitReviewTransferChecker,
  private val visitReviewRepository: VisitReviewRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
) {
  fun check(officialVisitId: Long, checkType: VisitReviewCheckType) {
    val officialVisit = officialVisitRepository.findById(officialVisitId).getOrNull() ?: return
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

  @Transactional
  fun expire(officialVisitId: Long) {
    visitReviewRepository.findByOfficialVisitId(officialVisitId).forEach(VisitReviewEntity::expire)
  }

  @Transactional
  fun visitCheck(officialVisitId: Long) {
    check(officialVisitId, VisitReviewCheckType.CHECK)

    visitReviewQueueRepository.findById(officialVisitId).ifPresent { queueEntry ->
      visitReviewQueueRepository.delete(queueEntry)
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
