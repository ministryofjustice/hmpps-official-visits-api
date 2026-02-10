package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.TimeSlotResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.time.LocalDateTime.now

@Service
class PrisonTimeSlotService(
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
) {
  fun create(request: CreateTimeSlotRequest, user: User): TimeSlotResponse = prisonTimeSlotRepository.saveAndFlush(request.toEntity(user.username)).toModel()

  fun update(prisonTimeSlotId: Long, request: UpdateTimeSlotRequest, user: User): TimeSlotResponse {
    val timeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }

    val changedTimeSlotEntity = timeSlotEntity.copy(
      dayCode = request.dayCode,
      startTime = request.startTime,
      endTime = request.endTime,
      effectiveDate = request.effectiveDate,
      expiryDate = request.expiryDate,
      updatedBy = user.username,
      updatedTime = now(),
    )
    return prisonTimeSlotRepository.save(changedTimeSlotEntity).toModel()
  }

  fun delete(prisonTimeSlotId: Long): TimeSlotResponse {
    val deleted = prisonTimeSlotRepository.findById(prisonTimeSlotId).orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    // check association with visit slot
    require(noVisitSlotsExistFor(prisonTimeSlotId)) {
      throw EntityInUseException("The prison time slot has one or more visit slots associated with it and cannot be deleted.")
    }
    prisonTimeSlotRepository.deleteById(prisonTimeSlotId)
    return deleted.toModel()
  }

  private fun noVisitSlotsExistFor(prisonTimeSlotId: Long): Boolean = !prisonVisitSlotRepository.existsByPrisonTimeSlotId(prisonTimeSlotId)
}
