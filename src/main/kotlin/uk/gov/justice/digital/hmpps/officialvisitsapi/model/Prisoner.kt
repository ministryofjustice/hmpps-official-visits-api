package uk.gov.justice.digital.hmpps.officialvisitsapi.model

import java.time.LocalDate

data class Prisoner(
  val prisonerNumber: String,
  val prisonCode: String,
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
)
