package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

val MOORLAND_PRISONER = Prisoner(MOORLAND, "123456", 1, "Fred", "Bloggs")
val PENTONVILLE_PRISONER = Prisoner(PENTONVILLE, "123456", 1, "Jane", "Bloggs")

data class Prisoner(val prison: String, val number: String, val bookingId: Long, val firstName: String = "<FIRST_NAME>", val lastName: String = "<LAST_NAME>")
