package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

val CONTACT_MOORLAND_PRISONER = prisonerContact(
  prisonerNumber = MOORLAND_PRISONER.number,
  contactId = 123,
  prisonerContactId = 456,
  type = "O",
  firstName = "John",
  lastName = "Doe",
  phoneNumber = "0987654321",
)

val CONTACT_MOORLAND_PRISONER_ADDED = prisonerContact(
  prisonerNumber = MOORLAND_PRISONER.number,
  contactId = 125,
  prisonerContactId = 458,
  type = "O",
  firstName = "John",
  lastName = "Doe",
  phoneNumber = "0987654321",
)
