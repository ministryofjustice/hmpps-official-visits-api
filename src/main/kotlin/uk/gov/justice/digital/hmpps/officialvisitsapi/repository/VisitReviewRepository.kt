package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity

@Repository
interface VisitReviewRepository : JpaRepository<VisitReviewEntity, Long> {
  fun findByOfficialVisitId(officialVisitId: Long): List<VisitReviewEntity>

  @Query(
    """
      SELECT vr
      FROM VisitReviewEntity vr, OfficialVisitEntity ov
      WHERE vr.officialVisitId = :officialVisitId
        AND ov.officialVisitId = :officialVisitId
        AND ov.prisonCode = :prisonCode
        AND vr.expiredTime IS NULL
    """,
  )
  fun findCurrentByOfficialVisitIdAndPrisonCode(officialVisitId: Long, prisonCode: String): VisitReviewEntity?
}
