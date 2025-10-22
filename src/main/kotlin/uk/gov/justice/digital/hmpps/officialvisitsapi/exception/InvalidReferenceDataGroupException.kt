package uk.gov.justice.digital.hmpps.officialvisitsapi.exception

import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup

class InvalidReferenceDataGroupException(requestedGroup: String) : Exception(""""$requestedGroup" is not a valid reference code group. Valid groups are ${ReferenceDataGroup.entries.filter { it.isDocumented }.joinToString(", ")}""")
