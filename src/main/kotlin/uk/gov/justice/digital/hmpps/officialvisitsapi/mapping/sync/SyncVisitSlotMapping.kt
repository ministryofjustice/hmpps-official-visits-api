package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncVisitSlot

fun PrisonVisitSlotEntity.toSyncModel(prisonCode: String): SyncVisitSlot = SyncVisitSlot(
  prisonTimeSlotId = this.prisonTimeSlotId,
  visitSlotId = this.prisonVisitSlotId,
  dpsLocationId = this.dpsLocationId,
  maxAdults = this.maxAdults,
  maxGroups = this.maxGroups,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
  prisonCode = prisonCode,
)

fun PrisonVisitSlotEntity.toSyncModel(): SyncVisitSlot = SyncVisitSlot(
  prisonTimeSlotId = this.prisonTimeSlotId,
  visitSlotId = this.prisonVisitSlotId,
  dpsLocationId = this.dpsLocationId,
  maxAdults = this.maxAdults,
  maxGroups = this.maxGroups,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun SyncCreateVisitSlotRequest.toEntity() = PrisonVisitSlotEntity(
  prisonVisitSlotId = 0L,
  prisonTimeSlotId = this.prisonTimeSlotId,
  dpsLocationId = this.dpsLocationId,
  maxAdults = this.maxAdults,
  maxGroups = this.maxGroups,
  maxVideoSessions = this.maxVideoSessions,
  createdTime = this.createdTime,
  createdBy = this.createdBy,
)
