package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitDeletionInfo

fun OfficialVisitEntity.toSyncModel(pve: PrisonerVisitedEntity?): SyncOfficialVisit = SyncOfficialVisit(
  officialVisitId = this.officialVisitId,
  visitDate = this.visitDate,
  startTime = this.startTime,
  endTime = this.endTime,
  prisonVisitSlotId = this.prisonVisitSlot.prisonVisitSlotId,
  dpsLocationId = this.dpsLocationId,
  prisonCode = this.prisonCode,
  prisonerNumber = this.prisonerNumber,
  statusCode = this.visitStatusCode,
  completionCode = this.completionCode,
  offenderBookId = this.offenderBookId,
  offenderVisitId = this.offenderVisitId,
  visitType = this.visitTypeCode,
  prisonerAttendance = pve?.attendanceCode,
  searchType = this.searchTypeCode,
  visitComments = this.prisonerNotes,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
  visitors = this.officialVisitors().toSyncModel(),
  overrideBanStaffUsername = this.overrideBanBy,
  visitOrderNumber = this.visitOrderNumber,
  visitorConcernNotes = this.visitorConcernNotes,
)

fun OfficialVisitEntity.toSyncModel(): SyncOfficialVisitDeletionInfo = SyncOfficialVisitDeletionInfo(
  officialVisitId = this.officialVisitId,
  prisonCode = this.prisonCode,
  prisonerNumber = this.prisonerNumber,
  visitors = this.officialVisitors().toSyncItemModel(),
)
