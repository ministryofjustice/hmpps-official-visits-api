package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
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
import kotlin.jvm.optionals.getOrNull

@Service
class VisitReviewService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val checker: VisitReviewChecker,
  private val visitReviewRepository: VisitReviewRepository,
  private val visitReviewQueueRepository: VisitReviewQueueRepository,
  private val timeSource: TimeSource,
  private val visitForReviewRepository: VisitForReviewRepository,
  private val officialVisitsRetrievalService: OfficialVisitsRetrievalService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private fun check(officialVisitId: Long, checkType: VisitReviewCheckType) {
    val officialVisit = officialVisitRepository.findById(officialVisitId).getOrNull() ?: return
    val today = timeSource.today()

    logger.info("Checking visit ID ${officialVisit.officialVisitId} Prison ${officialVisit.prisonCode} Date ${officialVisit.visitDate} Prisoner ${officialVisit.prisonerNumber} time source $today")

    when {
      officialVisit.visitStatusCode != VisitStatusType.SCHEDULED -> {
        return
      }
      officialVisit.visitDate < today -> {
        return
      }
      officialVisit.visitDate > today.plusDays(7) -> {
        return
      }
    }

    logger.info("Visit eligible for review checks - ID ${officialVisit.officialVisitId} visit date ${officialVisit.visitDate} status ${officialVisit.visitStatusCode}")

    /**
     * The 7 and 2 day checks currently do the same checks today.
     * They are separate because they may evolve differently, depending on user feedback.
     * The question - should the 2-day check re-raise previously acknowledged issues?
     */

    when (checkType) {
      VisitReviewCheckType.CHECK_ON_UPDATE -> {
        // Remove and replace existing review
        visitReviewRepository.deleteByOfficialVisitId(officialVisit.officialVisitId)
        visitReviewRepository.flush()
        checker.check(officialVisit)
      }
      VisitReviewCheckType.CHECK_2_DAYS, VisitReviewCheckType.CHECK_7_DAYS -> {
        // Retain any existing review and acknowledgement detail, if they exist
        checker.check(officialVisit)
      }
    }
  }

  /**
   * Called by the review expiry job - to expire any issues after the visit date has passed.
   */
  @Transactional
  fun expire(officialVisitId: Long) {
    visitReviewRepository.findByOfficialVisitId(officialVisitId).forEach(VisitReviewEntity::expire)
  }

  /**
   * Called by the process-visit-reviews job - to check visit candidates for potential issues.
   * After checking, it removes the visit candidate from the visit_review_queue table.
   */
  @Transactional
  fun visitCheck(officialVisitId: Long, type: VisitReviewCheckType) {
    check(officialVisitId, type)
    visitReviewQueueRepository.findByOfficialVisitId(officialVisitId)?.let(visitReviewQueueRepository::delete)
  }

  @Transactional(readOnly = true)
  fun countVisitsForReview(prisonCode: String): VisitsForReviewCountResponse = VisitsForReviewCountResponse(
    prisonCode = prisonCode,
    visitsForReviewCount = visitForReviewRepository.countVisitsForReview(
      prisonCode = prisonCode,
      fromDate = timeSource.today(),
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

  @Transactional(readOnly = true)
  fun getVisitsForReview(prisonCode: String, pageable: Pageable): PagedModel<VisitsForReviewResponse> {
    val fromDate = timeSource.today()
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
  CHECK_7_DAYS,
  CHECK_ON_UPDATE,
  CHECK_2_DAYS,
}
