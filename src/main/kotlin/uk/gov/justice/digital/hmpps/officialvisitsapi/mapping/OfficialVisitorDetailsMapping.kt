package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping

import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitorDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PersonalRelationshipsRDService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ReferenceDataService

fun OfficialVisitorEntity.toModel(referenceDataService: ReferenceDataService, personalRelationshipsRDService: PersonalRelationshipsRDService) = OfficialVisitorDetails(
  visitorTypeCode = this.visitorTypeCode,
  visitorTypeDescription = referenceDataService.getReferenceDataByGroupAndCode(ReferenceDataGroup.VISITOR_TYPE, this.visitorTypeCode.toString())?.description
    ?: this.visitorTypeCode.toString(),
  firstName = this.firstName,
  lastName = this.lastName,
  contactId = this.contactId,
  prisonerContactId = this.prisonerContactId,
  relationshipTypeCode = this.relationshipTypeCode,
  relationshipTypeDescription = referenceDataService.getReferenceDataByGroupAndCode(ReferenceDataGroup.RELATIONSHIP_TYPE, this.relationshipTypeCode!!.toString())?.description
    ?: this.relationshipTypeCode.toString(),
  relationshipCode = this.relationshipCode,
  relationshipDescription = personalRelationshipsRDService.getReferenceDataByCode(getRelationShipCode(this.relationshipTypeCode.toString()), this.relationshipCode!!)?.description,
  attendanceDescription = referenceDataService.getReferenceDataByGroupAndCode(ReferenceDataGroup.ATTENDANCE, this.attendanceCode.toString())?.description
    ?: this.attendanceCode.toString(),
  leadVisitor = this.leadVisitor,
  assistedVisit = this.assistedVisit,
  visitorNotes = this.visitorNotes,
  attendanceCode = attendanceCode,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
  offenderVisitVisitorId = this.offenderVisitVisitorId,
)

fun List<OfficialVisitorEntity>.toModel(referenceDataService: ReferenceDataService, personalRelationshipsRDService: PersonalRelationshipsRDService) = map { it.toModel(referenceDataService, personalRelationshipsRDService) }

private fun getRelationShipCode(relationshipTypeCode: String?) = if (relationshipTypeCode == "OFFICIAL") {
  ReferenceCodeGroup.OFFICIAL_RELATIONSHIP.toString()
} else {
  ReferenceCodeGroup.SOCIAL_RELATIONSHIP.toString()
}
