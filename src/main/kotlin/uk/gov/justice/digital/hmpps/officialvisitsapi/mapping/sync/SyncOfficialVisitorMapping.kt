package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SynOfficialVisitorDeletionInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitor

fun OfficialVisitorEntity.toSyncModel(): SyncOfficialVisitor = SyncOfficialVisitor(
  officialVisitorId = this.officialVisitorId,
  contactId = this.contactId,
  firstName = this.firstName,
  lastName = this.lastName,
  relationshipType = this.relationshipTypeCode,
  relationshipCode = this.relationshipCode,
  attendanceCode = this.attendanceCode,
  leadVisitor = this.leadVisitor,
  assistedVisit = this.assistedVisit,
  visitorNotes = this.visitorNotes,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun List<OfficialVisitorEntity>.toSyncModel(): List<SyncOfficialVisitor> = map { it.toSyncModel() }

fun OfficialVisitorEntity.toSyncItemModel(): SynOfficialVisitorDeletionInfo = SynOfficialVisitorDeletionInfo(
  officialVisitorId = this.officialVisitorId,
  contactId = this.contactId,
  createdBy = this.createdBy,
)

fun List<OfficialVisitorEntity>.toSyncItemModel(): List<SynOfficialVisitorDeletionInfo> = map { it.toSyncItemModel() }
