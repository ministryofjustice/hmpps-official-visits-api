package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PageMetadata
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PagedModelPrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.RestrictionTypeDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.RestrictionsSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner
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
  lastModifiedDate = LocalDateTime.now(),
  level = 2,
  leafLevel = true,
  locked = false,
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

fun prisonerContact(
  prisonerNumber: String,
  type: String,
  currentTerm: Boolean = true,
  isApprovedVisitor: Boolean = true,
  isRelationshipActive: Boolean = true,
  deceasedDate: LocalDate? = null,
  contactId: Long = 654321,
  prisonerContactId: Long = 123456,
) = PrisonerContactSummary(
  contactId = contactId,
  prisonerNumber = prisonerNumber,
  titleCode = "MR",
  titleDescription = "Mr",
  lastName = "Doe",
  firstName = "John",
  middleNames = "William",
  dateOfBirth = null,
  deceasedDate = deceasedDate,
  relationshipTypeCode = type,
  relationshipTypeDescription = "Friend",
  relationshipToPrisonerCode = "FRI",
  relationshipToPrisonerDescription = "Friend",
  flat = "Flat 1",
  property = "123",
  street = "Baker Street",
  area = "Marylebone",
  cityCode = "25343",
  postcode = "NW1 6XE",
  countryCode = "ENG",
  countryDescription = "England",
  isRelationshipActive = isRelationshipActive,
  isApprovedVisitor = isApprovedVisitor,
  isNextOfKin = true,
  isEmergencyContact = true,
  currentTerm = currentTerm,
  isStaff = true,
  restrictionSummary = RestrictionsSummary(setOf(RestrictionTypeDetails("Restricted", "Not allowed")), 1, 1),
  cityDescription = "",
  countyCode = "HM",
  countyDescription = "",
  noFixedAddress = true,
  primaryAddress = true,
  mailAddress = true,
  phoneType = "",
  phoneTypeDescription = "",
  phoneNumber = "",
  extNumber = "",
  comments = "",
  prisonerContactId = prisonerContactId,
)

fun pagedModelPrisonerContactSummary(
  prisonerNumber: String,
  type: String,
) = PagedModelPrisonerContactSummary(
  listOf(prisonerContact(prisonerNumber, type, true, true, true, null)),
  PageMetadata(1, 1, 5, 1),
)

fun pagedModelPrisonerContactSummary(vararg prisonerContacts: PrisonerContactSummary) = PagedModelPrisonerContactSummary(
  prisonerContacts.toList(),
  PageMetadata(1, 1, 5, 1),
)
