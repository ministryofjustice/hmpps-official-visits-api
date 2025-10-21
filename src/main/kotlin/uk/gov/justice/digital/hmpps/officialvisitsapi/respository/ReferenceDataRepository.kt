package uk.gov.justice.digital.hmpps.officialvisitsapi.respository

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup

@Repository
interface ReferenceDataRepository : JpaRepository<ReferenceDataEntity, Long> {
  fun findAllByGroupCodeEquals(groupCode: ReferenceDataGroup, sort: Sort): List<ReferenceDataEntity>
  fun findAllByGroupCodeAndEnabledEquals(groupCode: ReferenceDataGroup, enabled: Boolean, sort: Sort): List<ReferenceDataEntity>
  fun findByGroupCodeAndCode(groupCode: ReferenceDataGroup, code: String): ReferenceDataEntity?
}
