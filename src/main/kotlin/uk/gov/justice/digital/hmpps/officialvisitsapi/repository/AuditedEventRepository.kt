package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AuditedEventEntity
import java.time.LocalDateTime

@Repository
interface AuditedEventRepository : JpaRepository<AuditedEventEntity, Long> {

  @Query(
    value = """
    DELETE FROM AuditedEventEntity ae
    WHERE ae.prisonerNumber = :prisonerNumber
    """,
  )
  @Modifying
  fun deleteAllByPrisonerNumber(prisonerNumber: String)

  @Query(
    value = """
    SELECT ae FROM AuditedEventEntity ae
    WHERE ae.officialVisitId = :officialVisitId
    AND ae.eventDateTime > :afterDateTime
    AND (
      ae.summaryText = 'Official visit cancelled'
      OR (
        ae.summaryText = 'Update visit visit type and visit slot'
        AND (
          ae.detailText LIKE '%Visit date changed%'
          OR ae.detailText LIKE '%Start time changed%'
          OR ae.detailText LIKE '%End time changed%'
          OR ae.detailText LIKE '%Location changed%'
          OR ae.detailText LIKE '%Visit type changed%'
        )
      )
      OR (
        ae.summaryText = 'Update visit visitors'
        AND (
          ae.detailText LIKE '%Visitors added%'
          OR ae.detailText LIKE '%Visitors removed%'
          OR ae.detailText LIKE '%Visitors updated%'
        )
      )
    )
    ORDER BY ae.eventDateTime DESC
    """,
  )
  fun findRelevantAuditEventsAfter(
    officialVisitId: Long,
    afterDateTime: LocalDateTime,
    pageable: Pageable,
  ): List<AuditedEventEntity>
}
