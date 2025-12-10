package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import java.util.Optional

@Repository
interface OfficialVisitRepository : JpaRepository<OfficialVisitEntity, Long> {
  fun findByOfficialVisitIdAndPrisonCode(officialVisitId: Long, prisonCode: String): Optional<OfficialVisitEntity>
}
