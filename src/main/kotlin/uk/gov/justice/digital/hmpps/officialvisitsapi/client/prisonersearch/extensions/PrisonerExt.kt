package uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.extensions

import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.model.Prisoner

fun Prisoner.isReleased(): Boolean = status == "INACTIVE OUT"

fun Prisoner.isAtDifferentPrisonTo(prisonCode: String) = !isReleased() && prisonCode != prisonId

fun Prisoner.getFullName(): String = "$firstName $lastName"
