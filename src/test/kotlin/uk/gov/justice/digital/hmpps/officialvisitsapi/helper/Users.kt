package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService

val PENTONVILLE_PRISON_USER = prisonUser(caseloads = listOf(PENTONVILLE))
val MOORLAND_PRISON_USER = prisonUser(caseloads = listOf(MOORLAND))
val SERVICE_USER = UserService.getServiceAsUser()
