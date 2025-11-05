package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService

val PENTONVILLE_PRISON_USER = prisonUser(activeCaseLoadId = PENTONVILLE)
val SERVICE_USER = UserService.getServiceAsUser()
