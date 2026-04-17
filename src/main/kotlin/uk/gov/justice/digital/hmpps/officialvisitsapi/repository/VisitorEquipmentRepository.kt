package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitorEquipmentEntity

@Repository
interface VisitorEquipmentRepository : JpaRepository<VisitorEquipmentEntity, Long> {
  fun deleteAllByOfficialVisitor(officialVisitor: OfficialVisitorEntity)

  @Query(
    value = """
    DELETE FROM VisitorEquipmentEntity ve
    WHERE ve.officialVisitor IN (
      SELECT ove
      FROM OfficialVisitorEntity ove
      WHERE ove.officialVisit IN (
        SELECT ov
        FROM OfficialVisitEntity ov
        where ov.prisonerNumber = :prisonerNumber
      )
    )
    """,
  )
  @Modifying
  fun deleteAllByPrisonerNumber(prisonerNumber: String)
}
