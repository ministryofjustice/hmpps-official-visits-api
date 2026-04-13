package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import java.time.LocalDate
import java.time.LocalTime

@Repository
interface OfficialVisitorRepository : JpaRepository<OfficialVisitorEntity, Long> {
  fun deleteByOfficialVisit(officialVisitEntity: OfficialVisitEntity)

  @Query(
    value = """
      SELECT ov
      FROM OfficialVisitorEntity ov
      WHERE :contactId = ov.contactId
        AND :visitDate = ov.officialVisit.visitDate
        AND :startTime < ov.officialVisit.endTime
        AND :endTime > ov.officialVisit.startTime
        AND ov.officialVisit.visitStatusCode = 'SCHEDULED'
    """,
  )
  fun findScheduledOverlappingVisitsBy(contactId: Long, visitDate: LocalDate, startTime: LocalTime, endTime: LocalTime): List<OfficialVisitorEntity>

  @Query(
    value = """
    DELETE FROM OfficialVisitorEntity ove
    WHERE ove.officialVisit.prisonerNumber = :prisonerNumber
    """,
  )
  @Modifying(clearAutomatically = true)
  fun deleteAllByPrisonerNumber(prisonerNumber: String)
}
