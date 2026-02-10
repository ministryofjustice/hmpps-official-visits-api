package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity

@Repository
interface OfficialVisitorRepository : JpaRepository<OfficialVisitorEntity, Long> {
  fun deleteByOfficialVisit(officialVisitEntity: OfficialVisitEntity)
}
