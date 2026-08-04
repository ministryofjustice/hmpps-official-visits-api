package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.model.OtherPrisonerDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.model.PrisonerNonAssociations

fun prisonerNonAssociations(
  prisoner: Prisoner = MOORLAND_PRISONER,
  nonAssociations: List<PrisonerNonAssociation> = emptyList(),
) = PrisonerNonAssociations(
  prisonerNumber = prisoner.number,
  firstName = prisoner.firstName,
  lastName = prisoner.lastName,
  prisonId = prisoner.prison,
  prisonName = "Prison ${prisoner.prison}",
  cellLocation = "A-1-001",
  openCount = nonAssociations.count { it.isOpen }.toString(),
  closedCount = nonAssociations.count { it.isClosed }.toString(),
  nonAssociations = nonAssociations,
)

fun nonAssociation(
  otherPrisoner: Prisoner,
  id: Long = 1,
  reason: PrisonerNonAssociation.Reason = PrisonerNonAssociation.Reason.VIOLENCE,
  restrictionType: PrisonerNonAssociation.RestrictionType = PrisonerNonAssociation.RestrictionType.WING,
) = PrisonerNonAssociation(
  id = id,
  role = PrisonerNonAssociation.Role.VICTIM,
  roleDescription = "Victim",
  reason = reason,
  reasonDescription = reason.value.lowercase().replaceFirstChar(Char::uppercase),
  restrictionType = restrictionType,
  restrictionTypeDescription = restrictionType.value.lowercase().replaceFirstChar(Char::uppercase),
  comment = "Must be kept apart",
  authorisedBy = "TEST_USER",
  whenCreated = now(),
  whenUpdated = now(),
  updatedBy = "TEST_USER",
  isOpen = true,
  isClosed = false,
  otherPrisonerDetails = OtherPrisonerDetails(
    prisonerNumber = otherPrisoner.number,
    role = OtherPrisonerDetails.Role.PERPETRATOR,
    roleDescription = "Perpetrator",
    firstName = otherPrisoner.firstName,
    lastName = otherPrisoner.lastName,
    prisonId = otherPrisoner.prison,
    prisonName = "Prison ${otherPrisoner.prison}",
    cellLocation = "B-2-007",
  ),
)
