package uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.location
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock.LocationsInsidePrisonApiMockServer
import java.time.LocalDateTime
import java.util.UUID

class LocationsInsidePrisonApiClientTest {
  private val locationKey = "WWI-A-1-001"
  private val locationId = UUID.randomUUID()
  private val server = LocationsInsidePrisonApiMockServer().also { it.start() }
  private val client = LocationsInsidePrisonClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching location by id`() {
    val location = location(WANDSWORTH, locationKeySuffix = "A-1-001")
    server.stubGetLocationById(location)

    client.getLocationById(location.id)?.key isEqualTo locationKey
  }

  @Test
  fun `should return non-residential summary of visit locations`() {
    val locationWandsworth = location(WANDSWORTH, locationKeySuffix = "A-1-001")
    server.stubGetOfficialVisitLocationsAtPrison(WANDSWORTH, fakeOfficialVisitLocationsAtPrison(WANDSWORTH, "WWI-A-1-001"))
    client.getOfficialVisitLocationsAtPrison(WANDSWORTH).first().key isEqualTo locationWandsworth.key

    val locationPentonville = location(PENTONVILLE, locationKeySuffix = "B-2-002")
    server.stubGetOfficialVisitLocationsAtPrison(PENTONVILLE, fakeOfficialVisitLocationsAtPrison(PENTONVILLE, "PVI-B-2-002"))
    client.getOfficialVisitLocationsAtPrison(PENTONVILLE).first().key isEqualTo locationPentonville.key
  }

  @Test
  fun `should return locations by keys`() {
    server.stubLocationsByKeys(
      locationIds = listOf(UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")),
      locationsToReturn = listOf(
        location(prisonCode = MOORLAND, locationKeySuffix = MOORLAND, localName = "Visit place", id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")),
      ),
    )
    val locationList = client.getLocationsByIds(listOf(UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")))

    with(locationList.first()) {
      id isEqualTo UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")
      prisonId isEqualTo MOORLAND
      localName isEqualTo "Visit place"
    }
  }

  @AfterEach
  fun after() {
    server.stop()
  }

  private fun fakeOfficialVisitLocationsAtPrison(prisonCode: String, key: String) = listOf(
    Location(
      id = locationId,
      prisonId = prisonCode,
      localName = "A name",
      code = "Code",
      pathHierarchy = "A-1-1-1",
      locationType = Location.LocationType.VISITS,
      permanentlyInactive = false,
      status = Location.Status.ACTIVE,
      level = 3,
      key = key,
      active = true,
      locked = false,
      isResidential = false,
      leafLevel = true,
      topLevelId = UUID.randomUUID(),
      deactivatedByParent = false,
      lastModifiedBy = "XXX",
      lastModifiedDate = LocalDateTime.now().minusDays(1),
    ),
  )
}
