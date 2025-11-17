package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping

import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationship.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact

fun PrisonerContactSummary.toModel() = ApprovedContact(
  prisonerContactId = this.prisonerContactId,
  contactId = this.contactId,
  prisonerNumber = this.prisonerNumber,
  titleCode = this.titleCode,
  titleDescription = this.titleDescription,
  lastName = this.lastName,
  firstName = this.firstName,
  middleNames = this.middleNames,
  dateOfBirth = this.dateOfBirth,
  deceasedDate = this.deceasedDate,
  relationshipTypeCode = this.relationshipTypeCode,
  relationshipTypeDescription = this.relationshipTypeDescription,
  relationshipToPrisonerCode = this.relationshipToPrisonerCode,
  relationshipToPrisonerDescription = this.relationshipToPrisonerDescription ?: "",
  flat = this.flat,
  property = this.property,
  street = this.street,
  area = this.area,
  cityCode = this.cityCode,
  cityDescription = this.cityDescription,
  countyCode = this.countyCode,
  countyDescription = this.countyDescription,
  noFixedAddress = this.noFixedAddress,
  postcode = this.postcode,
  countryCode = this.countryCode,
  countryDescription = this.countryDescription,
  primaryAddress = this.primaryAddress,
  mailAddress = this.mailAddress,
  phoneType = this.phoneType,
  phoneTypeDescription = this.phoneTypeDescription,
  phoneNumber = this.phoneNumber,
  extNumber = this.extNumber,
  isApprovedVisitor = this.isApprovedVisitor,
  isNextOfKin = this.isNextOfKin,
  isEmergencyContact = this.isEmergencyContact,
  isRelationshipActive = this.isRelationshipActive,
  currentTerm = this.currentTerm,
  comments = this.comments,
  isStaff = this.isStaff,
  restrictionSummary = restrictionSummary,
)

fun List<PrisonerContactSummary>.toModel() = map { it.toModel() }
