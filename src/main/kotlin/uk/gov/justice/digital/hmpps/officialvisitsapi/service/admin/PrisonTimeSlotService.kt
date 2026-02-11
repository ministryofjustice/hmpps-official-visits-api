package uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import java.time.LocalDateTime

@Service
@Transactional
class PrisonTimeSlotService(
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
) {
  @Transactional(readOnly = true)
  fun getPrisonTimeSlotById(prisonTimeSlotId: Long): TimeSlotResponse {
    val prisonTimeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found") }
    return prisonTimeSlotEntity.toModel()
  }

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
      updatedTime = LocalDateTime.now(),
    )
    return prisonTimeSlotRepository.saveAndFlush(changedTimeSlotEntity).toModel()
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
