package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.admin

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot
import java.util.UUID

class PrisonTimeSlotIntegrationTest : IntegrationTestBase() {

  private val location = moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
    prisonerSearchApi().stubGetPrisonName(MOORLAND, MOORLAND_PRISONER)
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(prisonCode = MOORLAND, locations = listOf(location))
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should return all time slots summary for the prison for admin role`() {
    val summary = webTestClient.get()
      .uri("/admin/time-slots/prison/{prisonCode}?activeOnly=false", MOORLAND_PRISONER.prison)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(TimeSlotSummary::class.java)
      .returnResult().responseBody!!

    Assertions.assertThat(summary.prisonCode).isEqualTo(MOORLAND_PRISONER.prison)
    Assertions.assertThat(summary.prisonName).isEqualTo("A prison")
    Assertions.assertThat(summary.timeSlots).isNotEmpty
    assertTimeSlotWithResponse(summary.timeSlots[0].timeSlot)
    assertVisitSlotWithResponse(summary.timeSlots[0].visitSlots[0])
  }

  private fun assertTimeSlotWithResponse(model: TimeSlot) {
    Assertions.assertThat(model.prisonTimeSlotId).isEqualTo(1)
    Assertions.assertThat(model.prisonCode).isEqualTo("MDI")
    Assertions.assertThat(model.dayCode).isEqualTo(DayType.MON)
    Assertions.assertThat(model.startTime).isEqualTo("09:00")
    Assertions.assertThat(model.endTime).isEqualTo("10:00")
    Assertions.assertThat(model.effectiveDate).isEqualTo("2025-10-01")
    Assertions.assertThat(model.expiryDate).isNull()
    Assertions.assertThat(model.createdBy).isEqualTo("TIM")
    Assertions.assertThat(model.createdTime).isInThePast
    Assertions.assertThat(model.updatedTime).isNull()
    Assertions.assertThat(model.updatedBy).isNull()
  }

  private fun assertVisitSlotWithResponse(model: VisitSlot) {
    Assertions.assertThat(model.prisonTimeSlotId).isEqualTo(1)
    Assertions.assertThat(model.prisonCode).isEqualTo("MDI")
    Assertions.assertThat(model.prisonTimeSlotId).isEqualTo(1)
    Assertions.assertThat(model.dpsLocationId).isEqualTo(UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))
    Assertions.assertThat(model.locationDescription).isEqualTo("Moorland area 1")
    Assertions.assertThat(model.locationType).isEqualTo("VIDEO_LINK")
    Assertions.assertThat(model.locationMaxCapacity).isEqualTo(10)
    Assertions.assertThat(model.maxAdults).isEqualTo(10)
    Assertions.assertThat(model.maxGroups).isEqualTo(5)
    Assertions.assertThat(model.createdBy).isEqualTo("TIM")
    Assertions.assertThat(model.createdTime).isInThePast
    Assertions.assertThat(model.updatedTime).isNull()
    Assertions.assertThat(model.updatedBy).isNull()
  }
}
