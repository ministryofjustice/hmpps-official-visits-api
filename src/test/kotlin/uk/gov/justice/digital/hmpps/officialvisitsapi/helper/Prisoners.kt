package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

val MOORLAND_PRISONER = Prisoner(MOORLAND, "123456", 1)
val PENTONVILLE_PRISONER = Prisoner(PENTONVILLE, "123456", 1)

data class Prisoner(val prison: String, val number: String, val bookingId: Long)
