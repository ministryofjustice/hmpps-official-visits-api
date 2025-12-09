package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import java.util.Optional

@Repository
interface PrisonerVisitedRepository : JpaRepository<PrisonerVisitedEntity, Long> {

  @Query("SELECT pv from PrisonerVisitedEntity pv WHERE pv.officialVisit.officialVisitId = :officialVisitId ")
  fun findByOfficialVisitId(officialVisitId: Long): Optional<PrisonerVisitedEntity>
}
