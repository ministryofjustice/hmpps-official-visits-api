package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

val PENTONVILLE_PRISONER = Prisoner(PENTONVILLE, "123456")
val WANDSWORTH_PRISONER = Prisoner(WANDSWORTH, "123456")

data class Prisoner(val prison: String, val number: String)
