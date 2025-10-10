package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService

val PRISON_USER_BIRMINGHAM = prisonUser()
val PRISON_USER_PENTONVILLE = prisonUser(activeCaseLoadId = PENTONVILLE)
val PRISON_USER_RISLEY = prisonUser(activeCaseLoadId = RISLEY)
val PRISON_USER_WANDSWORTH = prisonUser(activeCaseLoadId = WANDSWORTH)
val SERVICE_USER = UserService.getServiceAsUser()
