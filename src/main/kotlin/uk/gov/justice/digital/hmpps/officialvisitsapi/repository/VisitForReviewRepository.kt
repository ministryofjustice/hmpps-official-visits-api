package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

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
}
