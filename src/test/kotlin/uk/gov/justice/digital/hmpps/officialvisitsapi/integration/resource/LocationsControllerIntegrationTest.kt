package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.location
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import java.util.UUID

class LocationsControllerIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get all official visit locations at prison returns list`() {
    val id = UUID.randomUUID()
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(WANDSWORTH, listOf(location(prisonCode = WANDSWORTH, locationKeySuffix = "A-1-001", localName = "Room 1", id = id)))

    webTestClient.get()
      .uri("/admin/prison/WWI/official-visit-locations")
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$[0].locationId").isEqualTo(id.toString())
      .jsonPath("$[0].locationName").isEqualTo("Room 1")
  }
}
