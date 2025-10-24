package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AvailableSlotEntity

/**
 * This repository is read-only and accessed via the view v_available_visit_slots.
 */

@Repository
interface AvailableSlotRepository : ReadOnlyRepository<AvailableSlotEntity, Long> {
  fun findAvailableSlotsByPrisonCode(prisonCode: String): List<AvailableSlotEntity>
}
