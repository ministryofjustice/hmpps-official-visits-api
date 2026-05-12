package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

val MOORLAND_PRISONER = Prisoner(MOORLAND, "123456", 1, "Fred", "Bloggs")
val PENTONVILLE_PRISONER = Prisoner(PENTONVILLE, "123456", 1, "Jane", "Bloggs")
val SWALESIDE_PRISONER = Prisoner(SWALESIDE, "A7654BC", 2, "Alice", "Smith")

data class Prisoner(val prison: String, val number: String, val bookingId: Long, val firstName: String = "<FIRST_NAME>", val lastName: String = "<LAST_NAME>")
