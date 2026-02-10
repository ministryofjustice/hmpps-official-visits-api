package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.TimeSlotResponse
import java.time.LocalDateTime.now

fun PrisonTimeSlotEntity.toModel(): TimeSlotResponse = TimeSlotResponse(
  prisonTimeSlotId = this.prisonTimeSlotId,
  prisonCode = this.prisonCode,
  dayCode = this.dayCode,
  startTime = this.startTime,
  endTime = this.endTime,
  effectiveDate = this.effectiveDate,
  expiryDate = this.expiryDate,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun CreateTimeSlotRequest.toEntity(userName: String) = PrisonTimeSlotEntity(
  prisonTimeSlotId = 0L,
  prisonCode = this.prisonCode,
  dayCode = this.dayCode,
  startTime = this.startTime,
  endTime = this.endTime,
  effectiveDate = this.effectiveDate,
  expiryDate = this.expiryDate,
  createdBy = userName,
  createdTime = now(),
)
