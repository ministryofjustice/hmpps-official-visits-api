package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AvailableSlotEntity
import java.time.LocalDate

/**
 * This repository is read-only and accessed via the view v_available_visit_slots.
 */

@Repository
interface AvailableSlotRepository : ReadOnlyRepository<AvailableSlotEntity, Long> {
  @Query(
    value = """
      FROM AvailableSlotEntity ase
      WHERE ase.prisonCode = :prisonCode 
        AND (ase.expiryDate is null or ase.expiryDate >= :fromDate)
    """,
  )
  fun findAvailableSlotsForPrison(prisonCode: String, fromDate: LocalDate): List<AvailableSlotEntity>

  @Query(
    value = """
      FROM AvailableSlotEntity ase
      WHERE ase.prisonCode = :prisonCode
        AND (ase.maxVideoSessions is not null and ase.maxVideoSessions > 0)
        AND (ase.expiryDate is null or ase.expiryDate >= :fromDate)
    """,
  )
  fun findAvailableVideoSlotsForPrison(prisonCode: String, fromDate: LocalDate): List<AvailableSlotEntity>
}
