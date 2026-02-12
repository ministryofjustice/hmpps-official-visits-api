package uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class PrisonTimeSlotService(
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
) {
  @Transactional(readOnly = true)
  fun getPrisonTimeSlotById(prisonTimeSlotId: Long): TimeSlot {
    val prisonTimeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    return prisonTimeSlotEntity.toModel()
  }

  fun create(request: CreateTimeSlotRequest, user: User): TimeSlot {
    request.validate()
    return prisonTimeSlotRepository.saveAndFlush(request.toEntity(user.username)).toModel()
  }

  fun update(prisonTimeSlotId: Long, request: UpdateTimeSlotRequest, user: User): TimeSlot {
    val timeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    request.validate()
    val changedTimeSlotEntity = timeSlotEntity.copy(
      dayCode = request.dayCode,
      startTime = request.startTime,
      endTime = request.endTime,
      effectiveDate = request.effectiveDate,
      expiryDate = request.expiryDate,
      updatedBy = user.username,
      updatedTime = LocalDateTime.now(),
    )
    return prisonTimeSlotRepository.saveAndFlush(changedTimeSlotEntity).toModel()
  }

  fun delete(prisonTimeSlotId: Long): TimeSlot {
    val deleted = prisonTimeSlotRepository.findById(prisonTimeSlotId).orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    // check association with visit slot
    require(noVisitSlotsExistFor(prisonTimeSlotId)) {
      throw EntityInUseException("The prison time slot has one or more visit slots associated with it and cannot be deleted.")
    }
    prisonTimeSlotRepository.deleteById(prisonTimeSlotId)
    return deleted.toModel()
  }

  private fun noVisitSlotsExistFor(prisonTimeSlotId: Long): Boolean = !prisonVisitSlotRepository.existsByPrisonTimeSlotId(prisonTimeSlotId)

  private fun UpdateTimeSlotRequest.validate() = also {
    require(startTime < endTime) { "Prison time slot start time must be before end time" }
    require(expiryDate == null || expiryDate >= LocalDate.now()) { "Prison time slot expiry date must not be in the past" }
  }

  private fun CreateTimeSlotRequest.validate() = also {
    require(startTime < endTime) { "Prison time slot start time must be before end time" }
    require(expiryDate == null || expiryDate >= LocalDate.now()) { "Prison time slot expiry date must not be in the past" }
  }
}
