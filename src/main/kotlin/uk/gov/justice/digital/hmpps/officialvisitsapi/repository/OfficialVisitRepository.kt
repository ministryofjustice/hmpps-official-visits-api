package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity

@Repository
interface OfficialVisitRepository : JpaRepository<OfficialVisitEntity, Long> {
  fun findByOfficialVisitIdAndPrisonCode(officialVisitId: Long, prisonCode: String): OfficialVisitEntity?

  @Query("SELECT ov.officialVisitId FROM OfficialVisitEntity ov WHERE (:currentTermOnly is null OR ov.currentTerm = :currentTermOnly)")
  fun findAllOfficialVisitIds(currentTermOnly: Boolean?, pageable: Pageable): Page<Long>
}
