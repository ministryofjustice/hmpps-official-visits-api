package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitorDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ReferenceDataService

fun OfficialVisitorEntity.toModel(referenceDataService: ReferenceDataService) = OfficialVisitorDetails(
  visitorTypeCode = this.visitorTypeCode,
  firstName = this.firstName,
  lastName = this.lastName,
  contactId = this.contactId,
  prisonerContactId = this.prisonerContactId,
  relationshipTypeCode = this.relationshipTypeCode,
  relationshipCode = this.relationshipCode,
  leadVisitor = this.leadVisitor,
  assistedVisit = this.assistedVisit,
  visitorNotes = this.visitorNotes,
  attendanceCode = attendanceCode,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
  offenderVisitVisitorId = this.offenderVisitVisitorId,
  attendanceDescription = referenceDataService.getReferenceDataByGroupAndCode(ReferenceDataGroup.ATTENDANCE, this.attendanceCode.toString())?.description
    ?: "",
)

fun List<OfficialVisitorEntity>.toModel(referenceDataService: ReferenceDataService) = map { it.toModel(referenceDataService) }
