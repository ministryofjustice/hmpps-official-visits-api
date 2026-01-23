package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot

fun PrisonTimeSlotEntity.toSyncModel(): SyncTimeSlot = SyncTimeSlot(
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

fun List<PrisonTimeSlotEntity>.toSyncModel(): List<SyncTimeSlot> = this.map { it.toSyncModel() }

fun SyncCreateTimeSlotRequest.toEntity() = PrisonTimeSlotEntity(
  prisonTimeSlotId = 0L,
  prisonCode = this.prisonCode,
  dayCode = this.dayCode,
  startTime = this.startTime,
  endTime = this.endTime,
  effectiveDate = this.effectiveDate,
  expiryDate = this.expiryDate,
  createdBy = createdBy,
  createdTime = createdTime,
)
