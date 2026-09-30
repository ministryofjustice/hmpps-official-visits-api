package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
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
  private val releaseChecker: VisitReviewReleaseChecker,
  private val transferChecker: VisitReviewTransferChecker,
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
        logger.info("Visit is not scheduled state ${officialVisit.visitStatusCode}")
        return
      }
      officialVisit.visitDate < today -> {
        logger.info("Visit was scheduled in the past ${officialVisit.visitDate}")
        return
      }
      officialVisit.visitDate > today.plusDays(7) -> {
        logger.info("Visit date is beyond 7 days from now - not checking or raising issues, visit date ${officialVisit.visitDate}, 7-days from now ${today.plusDays(7)}")
        return
      }
    }

    when (checkType) {
      VisitReviewCheckType.TRANSFER -> {
        logger.info("Check type is TRANSFER")
        transferChecker.check(officialVisit)
      }
      VisitReviewCheckType.RELEASE -> {
        logger.info("Check type is RELEASE")
        releaseChecker.check(officialVisit)
      }
      VisitReviewCheckType.UPDATE -> {
        logger.info("Check type is UPDATE")
        update(officialVisit)
      }
      VisitReviewCheckType.RECHECK, VisitReviewCheckType.CHECK -> {
        logger.info("Check type is $checkType")
        checker.check(officialVisit)
      }
    }
  }

  private fun update(officialVisit: OfficialVisitEntity) {
    logger.info("UPDATE check - removing any review items for ${officialVisit.officialVisitId}")
    visitReviewRepository.deleteByOfficialVisitId(officialVisit.officialVisitId)
    visitReviewRepository.flush()

    logger.info("UPDATE check - calling the standard check for ${officialVisit.officialVisitId}")
    checker.check(officialVisit)
  }

  @Transactional
  fun expire(officialVisitId: Long) {
    logger.info("EXPIRING official visit ID $officialVisitId - date must have passed now")
    visitReviewRepository.findByOfficialVisitId(officialVisitId).forEach(VisitReviewEntity::expire)
  }

  @Transactional
  fun visitCheck(officialVisitId: Long, type: VisitReviewCheckType) {
    check(officialVisitId, type)
    visitReviewQueueRepository.findByOfficialVisitId(officialVisitId)?.let(visitReviewQueueRepository::delete)
  }

  // TODO: Don't think this is necessary
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun visitCheckInNewTransaction(officialVisitId: Long, type: VisitReviewCheckType) {
    visitCheck(officialVisitId, type)
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
  CHECK,
  UPDATE,
  RECHECK,
  RELEASE,
  TRANSFER,
}
