package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity

@Repository
interface VisitReviewQueueRepository : JpaRepository<VisitReviewQueueEntity, Long> {
  @Query(
    """
      SELECT vrq
      FROM VisitReviewQueueEntity vrq
      JOIN OfficialVisitEntity ov ON ov.officialVisitId = vrq.officialVisitId
      WHERE vrq.createdTime = (
          SELECT MIN(vrq2.createdTime) FROM VisitReviewQueueEntity vrq2
          WHERE vrq2.officialVisitId = vrq.officialVisitId
      )
      ORDER BY vrq.createdTime ASC
    """,
  )
  fun findCandidatesOrderedByQueueTime(): Collection<VisitReviewQueueEntity>

  fun findByOfficialVisitId(officialVisitId: Long): VisitReviewQueueEntity?
}
