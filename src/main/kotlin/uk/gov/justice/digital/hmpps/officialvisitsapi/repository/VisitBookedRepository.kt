package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitBookedEntity
import java.time.LocalDate

/**
 * This repository is read-only and accessed via the view v_official_visits_booked.
 */

@Repository
interface VisitBookedRepository : ReadOnlyRepository<VisitBookedEntity, Long> {
  fun findVisitsBookedByPrisonCode(prisonCode: String): List<VisitBookedEntity>

  @Query(
    value = """
      FROM VisitBookedEntity vbe 
      WHERE vbe.prisonCode = :prisonCode
        AND vbe.visitDate BETWEEN :fromDate AND :toDate
        AND vbe.visitStatusCode != 'CANCELLED'
    """,
  )
  fun findCurrentVisitsBookedBy(prisonCode: String, fromDate: LocalDate, toDate: LocalDate): List<VisitBookedEntity>
}
