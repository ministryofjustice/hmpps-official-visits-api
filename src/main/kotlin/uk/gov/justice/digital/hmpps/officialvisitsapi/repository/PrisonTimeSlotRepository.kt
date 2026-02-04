package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity

@Repository
interface PrisonTimeSlotRepository : JpaRepository<PrisonTimeSlotEntity, Long> {
  fun findAllByPrisonCode(prisonCode: String): List<PrisonTimeSlotEntity>

  @Query(
    value = """
      SELECT ts
      FROM PrisonTimeSlotEntity ts 
      WHERE ts.prisonCode = :prisonCode 
      AND ts.effectiveDate <= CURRENT_DATE
      AND (ts.expiryDate IS NULL OR  ts.expiryDate >= CURRENT_DATE)
      """,
  )
  fun findAllActiveByPrisonCode(prisonCode: String): List<PrisonTimeSlotEntity>
}
