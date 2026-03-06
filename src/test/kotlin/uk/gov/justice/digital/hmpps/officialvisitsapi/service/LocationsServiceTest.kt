package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.location
import java.util.UUID

class LocationsServiceTest {
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val service = LocationsService(locationsInsidePrisonClient)

  @AfterEach
  fun afterEach() {
    reset(locationsInsidePrisonClient)
  }

  @Test
  fun `should return locations from client`() {
    val id = UUID.randomUUID()
    val loc = location(prisonCode = WANDSWORTH, locationKeySuffix = "A-1-001", localName = "Room A", id = id)
    whenever(locationsInsidePrisonClient.getOfficialVisitLocationsAtPrison(WANDSWORTH)).thenReturn(listOf(loc))

    val result = service.getOfficialVisitLocationsAtPrison(WANDSWORTH)

    assertThat(result).hasSize(1)
    assertThat(result.first().id).isEqualTo(id)
    assertThat(result.first().localName).isEqualTo("Room A")
  }
}
