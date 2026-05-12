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

// Three contacts used in the Swaleside integration test scenario (one visit, three visitors)
val CONTACT_SWALESIDE_PRISONER_1 = prisonerContact(
  prisonerNumber = SWALESIDE_PRISONER.number,
  contactId = 201,
  prisonerContactId = 301,
  type = "O",
  firstName = "Anna",
  lastName = "Jones",
)

val CONTACT_SWALESIDE_PRISONER_2 = prisonerContact(
  prisonerNumber = SWALESIDE_PRISONER.number,
  contactId = 202,
  prisonerContactId = 302,
  type = "O",
  firstName = "Brian",
  lastName = "Brown",
)

val CONTACT_SWALESIDE_PRISONER_3 = prisonerContact(
  prisonerNumber = SWALESIDE_PRISONER.number,
  contactId = 203,
  prisonerContactId = 303,
  type = "O",
  firstName = "Carol",
  lastName = "White",
)

