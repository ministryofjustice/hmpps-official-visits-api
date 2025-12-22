package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit

fun OfficialVisitEntity.toSyncModel(): SyncOfficialVisit = SyncOfficialVisit(
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
  visitors = this.officialVisitors().toSyncModel(),
)
