package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot

fun PrisonVisitSlotEntity.toVisitSlotModel(prisonCode: String): VisitSlot = VisitSlot(
  prisonTimeSlotId = this.prisonTimeSlotId,
  visitSlotId = this.prisonVisitSlotId,
  dpsLocationId = this.dpsLocationId,
  maxAdults = this.maxAdults,
  maxGroups = this.maxGroups,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  maxVideo = this.maxVideoSessions,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
  prisonCode = prisonCode,
)

fun List<PrisonVisitSlotEntity>.toVisitSlotListModel(prisonCode: String): List<VisitSlot> = this.map { it.toVisitSlotModel(prisonCode) }

fun PrisonTimeSlotEntity.toTimeSlotModel(): TimeSlot = TimeSlot(
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

fun List<PrisonTimeSlotEntity>.toTimeSlotListModel(): List<TimeSlot> = this.map { it.toTimeSlotModel() }
