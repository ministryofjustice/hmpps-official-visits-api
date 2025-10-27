package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AvailableSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AvailableSlotRepository

@Service
class AvailableSlotService(private val availableSlotRepository: AvailableSlotRepository) {
  // TODO: Map from entity to a model representation if exposing via API
  fun getAvailableSlotsForPrison(prisonCode: String): List<AvailableSlotEntity> = availableSlotRepository.findAvailableSlotsByPrisonCode(prisonCode)
}
