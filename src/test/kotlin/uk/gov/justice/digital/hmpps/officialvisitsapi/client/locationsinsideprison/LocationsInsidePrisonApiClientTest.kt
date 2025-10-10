package uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.LocationKeyValue
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.location
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock.LocationsInsidePrisonApiMockServer
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
  fun `should get matching location by key`() {
    server.stubGetLocationByKey(location(WANDSWORTH, locationKeySuffix = "A-1-001"))

    client.getLocationByKey(locationKey)!!.key isEqualTo locationKey
  }

  @Test
  fun `should get matching location by list of keys`() {
    server.stubPostLocationByKeys(listOf(LocationKeyValue(locationKey, locationId)))

    client.getLocationsByKeys(setOf(locationKey)).single().key isEqualTo locationKey
  }

  @Test
  fun `should only return leaf level video link locations`() {
    server.stubVideoLinkLocationsAtPrison(listOf(LocationKeyValue(wandsworthLocation.key, locationId)), leafLevel = true)
    client.getVideoLinkLocationsAtPrison(WANDSWORTH).single().key isEqualTo wandsworthLocation.key

    server.stubVideoLinkLocationsAtPrison(listOf(LocationKeyValue(pentonvilleLocation.key, UUID.randomUUID())), leafLevel = false)
    client.getVideoLinkLocationsAtPrison(PENTONVILLE) hasSize 0
  }

  @Test
  fun `should return all non-residential locations`() {
    server.stubNonResidentialAppointmentLocationsAtPrison(WANDSWORTH, wandsworthLocation.copy(leafLevel = false))
    client.getNonResidentialAppointmentLocationsAtPrison(WANDSWORTH).single().key isEqualTo wandsworthLocation.key

    server.stubNonResidentialAppointmentLocationsAtPrison(PENTONVILLE, pentonvilleLocation.copy(leafLevel = false))
    client.getNonResidentialAppointmentLocationsAtPrison(PENTONVILLE).single().key isEqualTo pentonvilleLocation.key
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}
