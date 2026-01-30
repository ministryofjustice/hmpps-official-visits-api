package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncVisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository

@Service
@Transactional
class SyncVisitSlotService(val prisonVisitSlotRepository: PrisonVisitSlotRepository, val prisonTimeSlotRepository: PrisonTimeSlotRepository, val officialVisitRepository: OfficialVisitRepository) {
  fun createPrisonVisitSlot(request: SyncCreateVisitSlotRequest): SyncVisitSlot {
    val timeSlotEntity = prisonTimeSlotRepository.findById(request.prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID ${request.prisonTimeSlotId} was not found for visit slot") }

    return prisonVisitSlotRepository.saveAndFlush(request.toEntity()).toSyncModel(timeSlotEntity.prisonCode)
  }

  fun updatePrisonVisitSlot(prisonVisitSlotId: Long, request: SyncUpdateVisitSlotRequest): SyncVisitSlot {
    val visitSlotEntity = prisonVisitSlotRepository.findById(prisonVisitSlotId)
      .orElseThrow { EntityNotFoundException("Prison visit slot with ID $prisonVisitSlotId was not found") }
    val changedVisitSlotEntity = visitSlotEntity.copy(
      dpsLocationId = request.dpsLocationId,
      maxAdults = request.maxAdults,
      maxGroups = request.maxGroups,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )
    val timeSlotEntity = prisonTimeSlotRepository.findById(changedVisitSlotEntity.prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID ${changedVisitSlotEntity.prisonTimeSlotId} was not found for visit slot") }

    return prisonVisitSlotRepository.saveAndFlush(changedVisitSlotEntity).toSyncModel(timeSlotEntity.prisonCode)
  }

  @Transactional(readOnly = true)
  fun getPrisonVisitSlotById(prisonVisitSlotId: Long): SyncVisitSlot {
    val prisonVisitSlotEntity = prisonVisitSlotRepository.findById(prisonVisitSlotId)
      .orElseThrow { EntityNotFoundException("Prison visit slot with ID $prisonVisitSlotId was not found") }

    val timeSlotEntity = prisonTimeSlotRepository.findById(prisonVisitSlotEntity.prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID ${prisonVisitSlotEntity.prisonTimeSlotId} was not found for visit slot") }
    return prisonVisitSlotEntity.toSyncModel(timeSlotEntity.prisonCode)
  }

  fun deletePrisonVisitSlot(prisonVisitSlotId: Long): SyncVisitSlot {
    val prisonVisitSlotEntity = prisonVisitSlotRepository.findById(prisonVisitSlotId)
      .orElseThrow { EntityNotFoundException("Prison visit slot with ID $prisonVisitSlotId was not found") }
    require(noVisitsExitsFor(prisonVisitSlotEntity.prisonVisitSlotId)) {
      throw EntityInUseException("The prison visit slot has visits associated with it and cannot be deleted.")
    }
    val timeSlotEntity = prisonTimeSlotRepository.findById(prisonVisitSlotEntity.prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID ${prisonVisitSlotEntity.prisonTimeSlotId} was not found for visit slot") }
    prisonVisitSlotRepository.deleteById(prisonVisitSlotId)
    return prisonVisitSlotEntity.toSyncModel(timeSlotEntity.prisonCode)
  }

  private fun noVisitsExitsFor(prisonVisitSlotId: Long): Boolean = !officialVisitRepository.existsByPrisonVisitSlotPrisonVisitSlotId(prisonVisitSlotId)
}
