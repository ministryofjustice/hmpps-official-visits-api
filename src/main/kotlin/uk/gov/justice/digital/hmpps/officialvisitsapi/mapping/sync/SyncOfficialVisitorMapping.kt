package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitor

fun OfficialVisitorEntity.toSyncModel(): SyncOfficialVisitor = SyncOfficialVisitor(
  officialVisitorId = this.officialVisitorId,
  contactId = this.contactId,
  firstName = this.firstName,
  lastName = this.lastName,
  relationshipType = this.relationshipTypeCode,
  relationshipCode = this.relationshipCode,
  attendanceCode = this.attendanceCode,
)

fun List<OfficialVisitorEntity>.toSyncModel(): List<SyncOfficialVisitor> = map { it.toSyncModel() }
