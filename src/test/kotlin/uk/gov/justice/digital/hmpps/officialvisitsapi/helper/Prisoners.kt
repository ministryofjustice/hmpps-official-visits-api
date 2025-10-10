package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

val pentonvillePrisoner = Prisoner(PENTONVILLE, "123456")
val wandsworthPrisoner = Prisoner(WANDSWORTH, "123456")

data class Prisoner(val prison: String, val number: String)
