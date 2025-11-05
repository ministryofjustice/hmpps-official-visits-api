package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.Prisoner as ModelPrisoner

val birminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "ABCEDFG", localName = "Birmingham room")
val inactiveBirminghamLocation = location(prisonCode = BIRMINGHAM, locationKeySuffix = "HIJLKLM", active = false)
val moorlandLocation = location(prisonCode = MOORLAND, locationKeySuffix = "ABCDEFG")
val pentonvilleLocation = location(prisonCode = PENTONVILLE, locationKeySuffix = "ABCDEFG", localName = "Pentonville room 3")
val risleyLocation = location(prisonCode = RISLEY, locationKeySuffix = "ABCDEFG", localName = "Risley room")
val wandsworthLocation = location(prisonCode = WANDSWORTH, locationKeySuffix = "ABCEDFG", localName = "Wandsworth room")

val allLocations = setOf(
  birminghamLocation,
  inactiveBirminghamLocation,
  moorlandLocation,
  pentonvilleLocation,
  risleyLocation,
  wandsworthLocation,
)

fun location(prisonCode: String, locationKeySuffix: String, active: Boolean = true, localName: String? = null, id: UUID = UUID.randomUUID()) = Location(
  id = id,
  prisonId = prisonCode,
  code = "VIDEOLINK",
  pathHierarchy = "VIDEOLINK",
  locationType = Location.LocationType.VIDEO_LINK,
  permanentlyInactive = false,
  active = active,
  deactivatedByParent = false,
  topLevelId = UUID.randomUUID(),
  key = "$prisonCode-$locationKeySuffix",
  isResidential = false,
  localName = localName,
  lastModifiedBy = "test user",
  lastModifiedDate = LocalDateTime.now().toIsoDateTime(),
  level = 2,
  leafLevel = true,
  status = Location.Status.ACTIVE,
)

fun prisonerSearchPrisoner(
  prisonerNumber: String,
  prisonCode: String,
  firstName: String = "Fred",
  lastName: String = "Bloggs",
  bookingId: Long = -1,
  lastPrisonCode: String? = null,
) = Prisoner(
  prisonerNumber = prisonerNumber,
  prisonId = prisonCode,
  firstName = firstName,
  lastName = lastName,
  bookingId = bookingId.toString(),
  dateOfBirth = LocalDate.of(2000, 1, 1),
  lastPrisonId = lastPrisonCode,
)

fun userDetails(
  username: String,
  name: String = "Test User",
  authSource: AuthSource = AuthSource.auth,
  activeCaseLoadId: String? = null,
  userId: String = "TEST",
) = UserDetailsDto(
  userId = userId,
  username = username,
  active = true,
  name = name,
  authSource = authSource,
  activeCaseLoadId = activeCaseLoadId,
)

fun serviceUser() = UserService.getServiceAsUser()

fun prisonUser(
  username: String = "prison_user",
  name: String = "Prison User",
  activeCaseLoadId: String = BIRMINGHAM,
) = PrisonUser(
  username = username,
  name = name,
  activeCaseLoadId = activeCaseLoadId,
)

fun prisoner(
  prisonerNumber: String,
  prisonCode: String,
  firstName: String = "Fred",
  lastName: String = "Bloggs",
) = ModelPrisoner(
  prisonerNumber = prisonerNumber,
  prisonCode = prisonCode,
  firstName = firstName,
  lastName = lastName,
  dateOfBirth = LocalDate.of(2000, 1, 1),
)
