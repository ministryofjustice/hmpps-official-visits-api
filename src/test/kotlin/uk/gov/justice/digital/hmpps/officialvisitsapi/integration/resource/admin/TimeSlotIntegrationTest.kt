package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.admin

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.TimeSlotInfo
import java.time.LocalDate
import java.time.LocalTime

class TimeSlotIntegrationTest : IntegrationTestBase() {
  private var savedPrisonTimeSlotId = 0L

  @BeforeEach
  fun initialiseData() {
    savedPrisonTimeSlotId = (webTestClient.createTimeSlot()).prisonTimeSlotId
    stubEvents.reset()
  }

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should create a new prison time slot`() {
    val timeslot = webTestClient.createTimeSlot()
    timeslot.assertWithCreateRequest(createTimeSlotRequest())

    stubEvents.assertHasEvent(
      event = OutboundEvent.TIME_SLOT_CREATED,
      additionalInfo = TimeSlotInfo(
        timeSlotId = timeslot.prisonTimeSlotId,
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_PRISON", "ROLE_OFFICIAL_VISITS__R", "ROLE_OFFICIAL_VISITS__RW"])
  fun `should return forbidden without an authorised role`(role: String) {
    webTestClient.post()
      .uri("/admin/time-slot")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createTimeSlotRequest())
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.put()
      .uri("/admin/time-slot/{prisonTimeSlotId}", 1L)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateTimeSlotRequest())
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.delete()
      .uri("/admin/time-slot/{prisonTimeSlotId}", 1L)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should return unauthorized if no token is provided`() {
    webTestClient.post()
      .uri("/admin/time-slot")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createTimeSlotRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.put()
      .uri("/admin/time-slot/{prisonTimeSlotId}", 1L)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateTimeSlotRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.delete()
      .uri("/admin/time-slot/{prisonTimeSlotId}", 1L)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should get an existing time slot by ID`() {
    val timeSlot = webTestClient.get()
      .uri("/admin/time-slot/{prisonTimeSlotId}", savedPrisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<TimeSlot>()
      .returnResult().responseBody!!

    timeSlot.assertWithCreateRequest(createTimeSlotRequest())
    assertThat(timeSlot.prisonTimeSlotId).isGreaterThan(0)
  }

  @Test
  fun `should update an existing time slot`() {
    val updateRequest = updateTimeSlotRequest()

    val timeSLot = webTestClient.put()
      .uri("/admin/time-slot/{prisonTimeSlotId}", savedPrisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN")))
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncTimeSlot>()
      .returnResult().responseBody!!

    Assertions.assertThat(timeSLot.prisonTimeSlotId).isEqualTo(savedPrisonTimeSlotId)
    Assertions.assertThat(timeSLot.dayCode).isEqualTo(updateRequest.dayCode)
    Assertions.assertThat(timeSLot.startTime).isEqualTo(updateRequest.startTime)
    Assertions.assertThat(timeSLot.endTime).isEqualTo(updateRequest.endTime)

    stubEvents.assertHasEvent(
      event = OutboundEvent.TIME_SLOT_UPDATED,
      additionalInfo = TimeSlotInfo(
        timeSlotId = timeSLot.prisonTimeSlotId,
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should fail to update time slot which does not exist`() {
    val updateRequest = updateTimeSlotRequest()

    webTestClient.put()
      .uri("/admin/time-slot/{prisonTimeSlotId}", 99L)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN")))
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody().jsonPath("$.userMessage").isEqualTo("Prison time slot with ID 99 was not found")
  }

  @Test
  fun `should delete a time slot if there are no associated visit slots`() {
    val timeSlot = webTestClient.createTimeSlot()
    stubEvents.reset()

    webTestClient.delete()
      .uri("/admin/time-slot/{prisonTimeSlotId}", timeSlot.prisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN")))
      .exchange()
      .expectStatus()
      .is2xxSuccessful

    stubEvents.assertHasEvent(
      event = OutboundEvent.TIME_SLOT_DELETED,
      additionalInfo = TimeSlotInfo(
        timeSlotId = timeSlot.prisonTimeSlotId,
        source = Source.DPS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should fail to delete time slot which has associated visit slots`() {
    webTestClient.delete()
      .uri("/admin/time-slot/{prisonTimeSlotId}", 1L)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody().jsonPath("$.userMessage").isEqualTo("The prison time slot has one or more visit slots associated with it and cannot be deleted.")
  }

  @Test
  fun `should fail to delete time slot which does not exist`() {
    webTestClient.delete()
      .uri("/admin/time-slot/{prisonTimeSlotId}", 99)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN")))
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody().jsonPath("$.userMessage").isEqualTo("Prison time slot with ID 99 was not found")
  }

  private fun TimeSlot.assertWithCreateRequest(request: CreateTimeSlotRequest) {
    Assertions.assertThat(prisonCode).isEqualTo(request.prisonCode)
    Assertions.assertThat(dayCode).isEqualTo(request.dayCode)
    Assertions.assertThat(startTime).isEqualTo(request.startTime)
    Assertions.assertThat(endTime).isEqualTo(request.endTime)
    Assertions.assertThat(effectiveDate).isEqualTo(request.effectiveDate)
    Assertions.assertThat(expiryDate).isEqualTo(request.expiryDate)
  }

  private fun createTimeSlotRequest() = CreateTimeSlotRequest(
    prisonCode = MOORLAND,
    dayCode = DayType.MON,
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    effectiveDate = LocalDate.now().plusDays(1),
    expiryDate = LocalDate.now().plusDays(365),
  )

  private fun updateTimeSlotRequest() = UpdateTimeSlotRequest(
    prisonCode = MOORLAND,
    dayCode = DayType.TUE,
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(12, 0),
    effectiveDate = LocalDate.now().plusDays(1),
    expiryDate = LocalDate.now().plusDays(365),
  )

  fun WebTestClient.createTimeSlot() = this.post()
    .uri("/admin/time-slot")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN")))
    .bodyValue(createTimeSlotRequest())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<TimeSlot>()
    .returnResult().responseBody!!
}
