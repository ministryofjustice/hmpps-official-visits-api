package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import java.time.LocalDate

@Repository
interface OfficialVisitRepository : JpaRepository<OfficialVisitEntity, Long> {
  fun findByOfficialVisitIdAndPrisonCode(officialVisitId: Long, prisonCode: String): OfficialVisitEntity?

  @Query("SELECT ov.officialVisitId FROM OfficialVisitEntity ov WHERE (:currentTermOnly is null OR ov.currentTerm = :currentTermOnly)")
  fun findAllOfficialVisitIds(currentTermOnly: Boolean?, pageable: Pageable): Page<Long>

  @Query(
    """
    SELECT ov 
    FROM OfficialVisitEntity ov 
    WHERE ov.prisonerNumber = :prisonerNumber
    AND (:currentTerm = false OR ov.currentTerm = true)
    AND CAST(:fromDate as date) IS NULL OR ov.visitDate >= :fromDate
    AND CAST(:toDate as date) IS NULL OR ov.visitDate <= :toDate
    ORDER BY ov.visitDate, ov.startTime
   """,
  )
  fun findAllPrisonerVisits(prisonerNumber: String, currentTerm: Boolean, fromDate: LocalDate?, toDate: LocalDate?): List<OfficialVisitEntity>
}
