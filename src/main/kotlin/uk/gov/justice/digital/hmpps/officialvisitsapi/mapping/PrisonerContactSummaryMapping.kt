package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping

import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationship.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact

fun PrisonerContactSummary.toModel() = ApprovedContact(
  prisonerContactId = prisonerContactId,
  contactId = contactId,
  prisonerNumber = prisonerNumber,
  lastName = lastName,
  firstName = firstName,
  relationshipTypeCode = relationshipTypeCode,
  relationshipTypeDescription = relationshipTypeDescription,
  relationshipToPrisonerCode = relationshipToPrisonerCode,
  relationshipToPrisonerDescription = relationshipToPrisonerDescription,
  isApprovedVisitor = isApprovedVisitor,
  isNextOfKin = isNextOfKin,
  isEmergencyContact = isEmergencyContact,
  isRelationshipActive = isRelationshipActive,
  currentTerm = currentTerm,
  isStaff = isStaff,
  restrictionSummary = restrictionSummary,
  titleCode = titleCode,
  titleDescription = titleDescription,
  middleNames = middleNames,
  dateOfBirth = dateOfBirth,
  deceasedDate = deceasedDate,
  flat = flat,
  property = property,
  street = street,
  area = area,
  cityCode = cityCode,
  cityDescription = cityDescription,
  countyCode = cityCode,
  countyDescription = countyDescription,
  postcode = postcode,
  countryCode = countryCode,
  countryDescription = countryDescription,
  noFixedAddress = noFixedAddress,
  primaryAddress = primaryAddress,
  mailAddress = mailAddress,
  phoneType = phoneType,
  phoneTypeDescription = phoneTypeDescription,
  phoneNumber = phoneNumber,
  extNumber = extNumber,
)

fun List<PrisonerContactSummary>.toModel() = map { it.toModel() }
