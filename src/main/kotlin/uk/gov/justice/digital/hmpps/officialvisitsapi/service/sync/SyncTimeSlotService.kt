package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.util.Optional

@Service
@Transactional
class SyncTimeSlotService(val prisonTimeSlotRepository: PrisonTimeSlotRepository, val prisonVisitSlotRepository: PrisonVisitSlotRepository) {
  @Transactional(readOnly = true)
  fun getPrisonTimeSlotById(prisonTimeSlotId: Long): SyncTimeSlot {
    val prisonTimeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    return prisonTimeSlotEntity.toSyncModel()
  }

  fun createPrisonTimeSlot(request: SyncCreateTimeSlotRequest) = prisonTimeSlotRepository.saveAndFlush(request.toEntity()).toSyncModel()

  fun updatePrisonTimeSlot(prisonTimeSlotId: Long, request: SyncUpdateTimeSlotRequest): SyncTimeSlot {
    val timeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }

    val changedTimeSlotEntity = timeSlotEntity.copy(
      dayCode = request.dayCode,
      startTime = request.startTime,
      endTime = request.endTime,
      effectiveDate = request.effectiveDate,
      expiryDate = request.expiryDate,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )

    return prisonTimeSlotRepository.saveAndFlush(changedTimeSlotEntity).toSyncModel()
  }

  fun deletePrisonTimeSlot(prisonTimeSlotId: Long) {
    val prisonTimeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    require(!validatePrisonVisitSlot(prisonTimeSlotEntity.prisonTimeSlotId)){
      throw EntityInUseException("Cannot delete prison Time slot as valid  visit slot associated with it")
    }
    prisonTimeSlotRepository.deleteById(prisonTimeSlotId)
  }

  private fun validatePrisonVisitSlot(prisonTimeSlotId: Long): Boolean =
    prisonVisitSlotRepository.existsByPrisonTimeSlotId(prisonTimeSlotId)
}


