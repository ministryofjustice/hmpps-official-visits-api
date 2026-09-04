package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitForReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitForReviewIssue
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitsForReviewCountResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitsForReviewResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitForReviewRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import java.time.LocalDate
import kotlin.collections.orEmpty
import kotlin.jvm.optionals.getOrNull

@Service
class VisitReviewService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val checker: VisitReviewChecker,
  private val releaseChecker: VisitReviewReleaseChecker,
  private val transferChecker: VisitReviewTransferChecker,
  private val visitReviewRepository: VisitReviewRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  private val timeSource: TimeSource,
  private val visitForReviewRepository: VisitForReviewRepository,
  private val officialVisitsRetrievalService: OfficialVisitsRetrievalService,

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

  fun countVisitsForReview(prisonCode: String): VisitsForReviewCountResponse = VisitsForReviewCountResponse(
    prisonCode = prisonCode,
    visitsForReviewCount = visitForReviewRepository.countVisitsForReview(
      prisonCode = prisonCode,
      fromDate = LocalDate.now(),
    ),
  )

  @Transactional
  fun acknowledgeVisitReview(prisonCode: String, officialVisitId: Long, user: User) {
    val visitReview = visitReviewRepository.findCurrentByOfficialVisitIdAndPrisonCode(officialVisitId, prisonCode)
      ?: throw EntityNotFoundException(
        "Visit review for official visit id $officialVisitId and prison code $prisonCode not found",
      )

    visitReview.updateAcknowledgedDetails(timeSource.now(), user.username)
  }

  fun getVisitsForReview(prisonCode: String, pageable: Pageable): PagedModel<VisitsForReviewResponse> {
    val fromDate = LocalDate.now()
    val visitIdsPage = visitForReviewRepository.findVisitIdsForReview(
      prisonCode = prisonCode,
      fromDate = fromDate,
      pageable = pageable,
    )

    if (visitIdsPage.isEmpty) {
      return PagedModel(PageImpl(emptyList(), pageable, visitIdsPage.totalElements))
    }

    val detailsByVisitId = visitForReviewRepository.findCurrentReviewDetailsForVisitIds(
      officialVisitIds = visitIdsPage.content,
      fromDate = fromDate,
    ).groupBy { it.officialVisitId }

    val response = visitIdsPage.content.map { officialVisitId ->
      VisitsForReviewResponse(
        visit = officialVisitsRetrievalService.getOfficialVisitByPrisonCodeAndId(prisonCode, officialVisitId),
        issues = detailsByVisitId[officialVisitId].orEmpty()
          .sortedBy { it.detailRaisedTime }
          .map { it.toIssue() },
      )
    }

    return PagedModel(PageImpl(response, pageable, visitIdsPage.totalElements))
  }

  private fun VisitForReviewEntity.toIssue() = VisitForReviewIssue(
    visitReviewDetailId = visitReviewDetailId,
    issueType = issueType,
    issueDetail = issueDetail,
    raisedTime = detailRaisedTime,
  )
}

enum class VisitReviewCheckType {
  CHECK,
  RECHECK,
  UPDATE,
  RELEASE,
  TRANSFER,
}
