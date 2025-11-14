package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitBookedEntity
import java.time.LocalDate

/**
 * This repository is read-only and accessed via the view v_official_visits_booked.
 */

@Repository
interface VisitBookedRepository : ReadOnlyRepository<VisitBookedEntity, Long> {
  fun findVisitsBookedByPrisonCode(prisonCode: String): List<VisitBookedEntity>

  fun findVisitBookedEntityByPrisonCodeAndVisitDateBetween(prisonCode: String, from: LocalDate, to: LocalDate): List<VisitBookedEntity>
}
