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

  @Query("SELECT ov.officialVisitId from OfficialVisitEntity ov WHERE ov.currentTerm = :currentTerm")
  fun findAllOfficialVisitIds(currentTerm: Boolean, pageable: Pageable): Page<Long>
}
