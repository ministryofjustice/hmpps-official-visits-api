package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitForReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import java.time.LocalDate

/**
 * This repository is read-only and accessed via the view v_visits_for_review.
 */
@Repository
interface VisitForReviewRepository : ReadOnlyRepository<VisitForReviewEntity, Long> {
  @Query(
    """
      SELECT COUNT(vfr)
      FROM VisitForReviewEntity vfr
      WHERE vfr.prisonCode = :prisonCode
        AND vfr.visitStatusCode = :visitStatus
        AND vfr.visitDate >= :fromDate
        AND vfr.acknowledgedTime IS NULL
        AND vfr.expiredTime IS NULL
    """,
  )
  fun countVisitsForReview(
    prisonCode: String,
    visitStatus: VisitStatusType = VisitStatusType.SCHEDULED,
    fromDate: LocalDate = LocalDate.now(),
  ): Long

  @Query(
    value = """
      SELECT vfr.officialVisitId
      FROM VisitForReviewEntity vfr
      WHERE vfr.prisonCode = :prisonCode
        AND vfr.visitStatusCode = :visitStatus
        AND vfr.visitDate >= :fromDate
        AND vfr.acknowledgedTime IS NULL
        AND vfr.expiredTime IS NULL
      GROUP BY vfr.officialVisitId, vfr.visitDate, vfr.startTime, vfr.endTime
    """,
    countQuery = """
      SELECT COUNT(DISTINCT vfr.officialVisitId)
      FROM VisitForReviewEntity vfr
      WHERE vfr.prisonCode = :prisonCode
        AND vfr.visitStatusCode = :visitStatus
        AND vfr.visitDate >= :fromDate
        AND vfr.acknowledgedTime IS NULL
        AND vfr.expiredTime IS NULL
    """,
  )
  fun findVisitIdsForReview(
    prisonCode: String,
    visitStatus: VisitStatusType = VisitStatusType.SCHEDULED,
    fromDate: LocalDate = LocalDate.now(),
    pageable: Pageable,
  ): Page<Long>

  @Query(
    """
      SELECT vfr
      FROM VisitForReviewEntity vfr
      WHERE vfr.officialVisitId IN :officialVisitIds
        AND vfr.visitStatusCode = :visitStatus
        AND vfr.visitDate >= :fromDate
        AND vfr.acknowledgedTime IS NULL
        AND vfr.expiredTime IS NULL
    """,
  )
  fun findCurrentReviewDetailsForVisitIds(
    officialVisitIds: List<Long>,
    visitStatus: VisitStatusType = VisitStatusType.SCHEDULED,
    fromDate: LocalDate = LocalDate.now(),
  ): List<VisitForReviewEntity>
}
