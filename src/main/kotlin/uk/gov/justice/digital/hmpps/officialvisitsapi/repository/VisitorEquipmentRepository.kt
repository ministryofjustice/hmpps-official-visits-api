package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitorEquipmentEntity

@Repository
interface VisitorEquipmentRepository : JpaRepository<VisitorEquipmentEntity, Long> {
  fun deleteAllByOfficialVisitor(officialVisitor: OfficialVisitorEntity)
}
