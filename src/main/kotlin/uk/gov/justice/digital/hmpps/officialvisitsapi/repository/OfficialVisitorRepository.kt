package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import java.time.LocalDate
import java.time.LocalTime

@Repository
interface OfficialVisitorRepository : JpaRepository<OfficialVisitorEntity, Long> {
  fun deleteByOfficialVisit(officialVisitEntity: OfficialVisitEntity)

  fun findByContactId(contactId: Long): OfficialVisitorEntity?

  @Query(
    value = """
      SELECT ov
      FROM OfficialVisitorEntity ov
      WHERE :visitor = ov
        AND :visitDate = ov.officialVisit.visitDate
        AND :startTime < ov.officialVisit.endTime
        AND :endTime > ov.officialVisit.startTime
        AND ov.officialVisit.visitStatusCode = 'SCHEDULED'
    """,
  )
  fun findScheduledOverlappingVisitsBy(visitor: OfficialVisitorEntity, visitDate: LocalDate, startTime: LocalTime, endTime: LocalTime): List<OfficialVisitorEntity>
}
