package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.location
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService
import java.util.UUID

class LocationsFacadeTest {
  private val locationsService: LocationsService = mock()
  private val facade = LocationsFacade(locationsService)

  @AfterEach
  fun afterEach() {
    reset(locationsService)
  }

  @Test
  fun `should map locations to VisitLocation response`() {
    val id = UUID.randomUUID()
    val loc = location(prisonCode = WANDSWORTH, locationKeySuffix = "A-1-001", localName = "Room A", id = id)
    whenever(locationsService.getOfficialVisitLocationsAtPrison(WANDSWORTH)).thenReturn(listOf(loc))

    // call with same prison code used for the mocked location
    val result = facade.getOfficialVisitLocationsAtPrison(WANDSWORTH)

    assertThat(result).hasSize(1)
    assertThat(result.first().locationId).isEqualTo(id)
    assertThat(result.first().locationName).isEqualTo("Room A")
  }
}
