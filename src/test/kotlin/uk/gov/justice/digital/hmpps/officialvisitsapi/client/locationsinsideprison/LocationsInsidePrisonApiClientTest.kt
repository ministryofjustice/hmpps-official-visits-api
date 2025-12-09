package uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.NonResidentialLocationDTO
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.NonResidentialSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.PageNonResidentialLocationDTO
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.location
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
  fun `should return non-residential summary of visit locations`() {
    val locationWandsworth = location(WANDSWORTH, locationKeySuffix = "A-1-001")
    server.stubGetNonResidentialOfficialVisitLocationsAtPrison(WANDSWORTH, fakeNonResidentialSummary(WANDSWORTH, "WWI-A-1-001"))
    client.getNonResidentialOfficialVisitLocationsAtPrison(WANDSWORTH)?.locations?.content?.first()?.key isEqualTo locationWandsworth.key

    val locationPentonville = location(PENTONVILLE, locationKeySuffix = "B-2-002")
    server.stubGetNonResidentialOfficialVisitLocationsAtPrison(PENTONVILLE, fakeNonResidentialSummary(PENTONVILLE, "PVI-B-2-002"))
    client.getNonResidentialOfficialVisitLocationsAtPrison(PENTONVILLE)?.locations?.content?.first()?.key isEqualTo locationPentonville.key
  }

  @AfterEach
  fun after() {
    server.stop()
  }

  private fun fakeNonResidentialSummary(prisonCode: String, key: String) = NonResidentialSummary(
    prisonId = prisonCode,
    locations = PageNonResidentialLocationDTO(
      totalElements = 1,
      totalPages = 1,
      number = 1,
      first = true,
      last = true,
      numberOfElements = 1,
      empty = false,
      content = listOf(
        NonResidentialLocationDTO(
          id = locationId,
          prisonId = prisonCode,
          localName = "A name",
          code = "Code",
          pathHierarchy = "A-1-1-1",
          locationType = NonResidentialLocationDTO.LocationType.VISITS,
          permanentlyInactive = false,
          usedByGroupedServices = listOf(NonResidentialLocationDTO.UsedByGroupedServices.OFFICIAL_VISITS),
          usedByServices = listOf(NonResidentialLocationDTO.UsedByServices.OFFICIAL_VISITS),
          status = NonResidentialLocationDTO.Status.ACTIVE,
          level = 3,
          key = key,
        ),
      ),
    ),
  )
}
