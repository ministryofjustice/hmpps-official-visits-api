package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
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

    assertThat(summary.prisonCode).isEqualTo(MOORLAND_PRISONER.prison)
    assertThat(summary.prisonName).isEqualTo("A prison")
    assertThat(summary.timeSlots).isNotEmpty
    assertTimeSlotWithResponse(summary.timeSlots[0].timeSlot)
    assertVisitSlotWithResponse(summary.timeSlots[0].visitSlots[0])
  }

  private fun assertTimeSlotWithResponse(model: TimeSlot) {
    assertThat(model.prisonTimeSlotId).isEqualTo(1)
    assertThat(model.prisonCode).isEqualTo("MDI")
    assertThat(model.dayCode).isEqualTo(DayType.MON)
    assertThat(model.startTime).isEqualTo("09:00")
    assertThat(model.endTime).isEqualTo("10:00")
    assertThat(model.effectiveDate).isEqualTo("2025-10-01")
    assertThat(model.expiryDate).isNull()
    assertThat(model.createdBy).isEqualTo("TIM")
    assertThat(model.createdTime).isInThePast
    assertThat(model.updatedTime).isNull()
    assertThat(model.updatedBy).isNull()
  }

  private fun assertVisitSlotWithResponse(model: VisitSlot) {
    assertThat(model.prisonTimeSlotId).isEqualTo(1)
    assertThat(model.prisonCode).isEqualTo("MDI")
    assertThat(model.prisonTimeSlotId).isEqualTo(1)
    assertThat(model.dpsLocationId).isEqualTo(UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))
    assertThat(model.locationDescription).isEqualTo("Moorland area 1")
    assertThat(model.locationType).isEqualTo("VIDEO_LINK")
    assertThat(model.locationMaxCapacity).isEqualTo(10)
    assertThat(model.maxAdults).isEqualTo(10)
    assertThat(model.maxGroups).isEqualTo(5)
    assertThat(model.createdBy).isEqualTo("TIM")
    assertThat(model.createdTime).isInThePast
    assertThat(model.updatedTime).isNull()
    assertThat(model.updatedBy).isNull()
  }
}
