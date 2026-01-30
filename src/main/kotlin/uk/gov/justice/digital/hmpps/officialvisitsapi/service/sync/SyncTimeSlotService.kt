package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository

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

  fun deletePrisonTimeSlot(prisonTimeSlotId: Long): SyncTimeSlot {
    val prisonTimeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    require(noVisitSlotsExistFor(prisonTimeSlotEntity.prisonTimeSlotId)) {
      throw EntityInUseException("The prison time slot has one or more visit slots associated with it and cannot be deleted.")
    }
    prisonTimeSlotRepository.deleteById(prisonTimeSlotId)
    return prisonTimeSlotEntity.toSyncModel()
  }

  private fun noVisitSlotsExistFor(prisonTimeSlotId: Long): Boolean = !prisonVisitSlotRepository.existsByPrisonTimeSlotId(prisonTimeSlotId)
}
