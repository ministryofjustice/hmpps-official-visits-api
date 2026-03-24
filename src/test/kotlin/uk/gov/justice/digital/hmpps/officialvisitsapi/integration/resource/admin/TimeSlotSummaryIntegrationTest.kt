package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummaryItem
import java.util.UUID

class TimeSlotSummaryIntegrationTest : IntegrationTestBase() {

  private val location = moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(prisonCode = MOORLAND, locations = listOf(location))
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should return time slot summary by ID with associated visit slots for admin role`() {
    val summary = webTestClient.get()
      .uri("/admin/time-slot/{prisonTimeSlotId}/summary", 1L)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<TimeSlotSummaryItem>()
      .returnResult().responseBody!!

    assertThat(summary.timeSlot.prisonTimeSlotId).isEqualTo(1L)
    assertThat(summary.timeSlot.prisonCode).isEqualTo(MOORLAND)
    assertThat(summary.timeSlot.dayCode).isEqualTo(DayType.MON)
    assertThat(summary.timeSlot.startTime).isEqualTo("09:00")
    assertThat(summary.timeSlot.endTime).isEqualTo("10:00")
    assertThat(summary.timeSlot.effectiveDate).isEqualTo("2025-10-01")
    assertThat(summary.timeSlot.expiryDate).isNull()
    assertThat(summary.timeSlot.createdBy).isEqualTo("TIM")
    assertThat(summary.timeSlot.updatedBy).isNull()

    assertThat(summary.visitSlots).isNotEmpty
    assertThat(summary.visitSlots[0].prisonTimeSlotId).isEqualTo(1L)
    assertThat(summary.visitSlots[0].prisonCode).isEqualTo(MOORLAND)
    assertThat(summary.visitSlots[0].dpsLocationId).isEqualTo(UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))
    assertThat(summary.visitSlots[0].locationDescription).isEqualTo("Moorland area 1")
    assertThat(summary.visitSlots[0].maxAdults).isEqualTo(10)
    assertThat(summary.visitSlots[0].maxGroups).isEqualTo(5)
    assertThat(summary.visitSlots[0].createdBy).isEqualTo("TIM")
    assertThat(summary.visitSlots[0].updatedBy).isNull()
  }

  @Test
  fun `should return not found when time slot does not exist`() {
    webTestClient.get()
      .uri("/admin/time-slot/{prisonTimeSlotId}/summary", 99L)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody().jsonPath("$.userMessage").isEqualTo("Prison time slot with ID 99 was not found")
  }

  @Test
  fun `should return unauthorized if no token is provided`() {
    webTestClient.get()
      .uri("/admin/time-slot/{prisonTimeSlotId}/summary", 1L)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_PRISON", "ROLE_OFFICIAL_VISITS__R", "ROLE_OFFICIAL_VISITS__RW"])
  fun `should return forbidden without an authorised role`(role: String) {
    webTestClient.get()
      .uri("/admin/time-slot/{prisonTimeSlotId}/summary", 1L)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
