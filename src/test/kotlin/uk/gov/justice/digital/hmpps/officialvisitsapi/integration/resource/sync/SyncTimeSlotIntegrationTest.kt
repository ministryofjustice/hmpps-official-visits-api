package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncTimeSlotSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.TimeSlotInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class SyncTimeSlotIntegrationTest : IntegrationTestBase() {
  private var savedPrisonTimeSlotId = 0L

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @BeforeEach
  fun initialiseData() {
    savedPrisonTimeSlotId = (webTestClient.createTimeSlot()).prisonTimeSlotId
    stubEvents.reset()
  }

  @Test
  fun `should return unauthorized if no token is provided`() {
    webTestClient.get()
      .uri("/sync/time-slot/{prisonTimeSlotId}", savedPrisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.post()
      .uri("/sync/tim-slot")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createTimeSlotRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.put()
      .uri("/sync/time-slot/{prisonTimeSlotId}", savedPrisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateTimeSlotRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_PRISON", "ROLE_OFFICIAL_VISITS__R", "ROLE_OFFICIAL_VISITS__RW"])
  fun `should return forbidden without an authorised role`(role: String) {
    webTestClient.get()
      .uri("/sync/time-slot/{prisonTimeSlotId}", savedPrisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.post()
      .uri("/sync/time-slot")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createTimeSlotRequest())
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.put()
      .uri("/sync/time-slot/1{prisonTimeSlotId}", savedPrisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateTimeSlotRequest())
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should get an existing time slot by ID`() {
    val syncTimeSlot = webTestClient.get()
      .uri("/sync/time-slot/{prisonTimeSlotId}", savedPrisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncTimeSlot>()
      .returnResult().responseBody!!

    syncTimeSlot.assertWithCreateRequest(createTimeSlotRequest())
    assertThat(syncTimeSlot.prisonTimeSlotId).isGreaterThan(0)
  }

  @Test
  fun `should create a new prison time slot`() {
    val syncTimeSlot = webTestClient.createTimeSlot()

    syncTimeSlot.assertWithCreateRequest(createTimeSlotRequest())
    assertThat(syncTimeSlot.prisonTimeSlotId).isGreaterThan(0)

    stubEvents.assertHasEvent(
      event = OutboundEvent.TIME_SLOT_CREATED,
      additionalInfo = TimeSlotInfo(
        timeSlotId = syncTimeSlot.prisonTimeSlotId,
        source = Source.NOMIS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should update an existing time slot`() {
    val updateRequest = updateTimeSlotRequest()

    val syncTimeSlot = webTestClient.put()
      .uri("/sync/time-slot/{prisonTimeSlotId}", savedPrisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncTimeSlot>()
      .returnResult().responseBody!!

    // Ensure the changes have been applied but the ID is the same
    assertThat(syncTimeSlot.prisonTimeSlotId).isEqualTo(savedPrisonTimeSlotId)
    assertThat(syncTimeSlot.dayCode).isEqualTo(updateRequest.dayCode)
    assertThat(syncTimeSlot.startTime).isEqualTo(updateRequest.startTime)
    assertThat(syncTimeSlot.endTime).isEqualTo(updateRequest.endTime)

    stubEvents.assertHasEvent(
      event = OutboundEvent.TIME_SLOT_UPDATED,
      additionalInfo = TimeSlotInfo(
        timeSlotId = syncTimeSlot.prisonTimeSlotId,
        source = Source.NOMIS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should delete time slot if there are no associated visit slots`() {
    val timeSLot = webTestClient.createTimeSlot()
    webTestClient.delete()
      .uri("/sync/time-slot/{prisonTimeSlotId}", timeSLot.prisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .is2xxSuccessful
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncTimeSlot>()
    stubEvents.assertHasEvent(
      event = OutboundEvent.TIME_SLOT_CREATED,
      additionalInfo = TimeSlotInfo(
        timeSlotId = timeSLot.prisonTimeSlotId,
        source = Source.NOMIS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.TIME_SLOT_DELETED,
      additionalInfo = TimeSlotInfo(
        timeSlotId = timeSLot.prisonTimeSlotId,
        source = Source.NOMIS,
        username = MOORLAND_PRISON_USER.username,
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should fail to delete time slot which does not exist`() {
    webTestClient.delete()
      .uri("/sync/time-slot/{prisonTimeSlotId}", 99)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody().jsonPath("$.userMessage").isEqualTo("Prison time slot with ID 99 was not found")
  }

  @Test
  fun `should fail to delete time slot which has associated visit slots`() {
    webTestClient.delete()
      .uri("/sync/time-slot/{prisonTimeSlotId}", 1L)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody().jsonPath("$.userMessage").isEqualTo("The prison time slot has one or more visit slots associated with it and cannot be deleted.")
  }

  @Test
  fun `should return all active time slots summary for the prison`() {
    val summary = webTestClient.get()
      .uri("/sync/time-slots/prison/{prisonCode}", "MDI")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncTimeSlotSummary>()
      .returnResult().responseBody!!
    assertThat(summary.prisonCode).isEqualTo("MDI")
    assertThat(summary.timeSlots).size().isEqualTo(9)
  }

  @Test
  fun `should return all time slot summary for the prison`() {
    val summary = webTestClient.get()
      .uri("/sync/time-slots/prison/{prisonCode}?activeOnly=false", "MDI")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncTimeSlotSummary>()
      .returnResult().responseBody!!
    assertThat(summary.prisonCode).isEqualTo("MDI")
    assertThat(summary.timeSlots).size().isEqualTo(16)
  }

  @Test
  fun `should return Zero time slot summary if there is no time slots associated with the prison code`() {
    val summary = webTestClient.get()
      .uri("/sync/time-slots/prison/{prisonCode}", "MDIN")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncTimeSlotSummary>()
      .returnResult().responseBody!!
    assertThat(summary.prisonCode).isEqualTo("MDIN")
    assertThat(summary.timeSlots).size().isEqualTo(0)
  }

  private fun createTimeSlotRequest() = SyncCreateTimeSlotRequest(
    prisonCode = MOORLAND,
    dayCode = DayType.MON,
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    effectiveDate = LocalDate.now().plusDays(1),
    expiryDate = LocalDate.now().plusDays(365),
    createdBy = MOORLAND_PRISON_USER.username,
    createdTime = createdTime,
  )

  private fun updateTimeSlotRequest() = SyncUpdateTimeSlotRequest(
    prisonCode = MOORLAND,
    dayCode = DayType.TUE,
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(12, 0),
    effectiveDate = LocalDate.now().plusDays(1),
    expiryDate = LocalDate.now().plusDays(365),
    updatedBy = MOORLAND_PRISON_USER.username,
    updatedTime = updatedTime,
  )

  private fun SyncTimeSlot.assertWithCreateRequest(request: SyncCreateTimeSlotRequest) {
    assertThat(prisonCode).isEqualTo(request.prisonCode)
    assertThat(dayCode).isEqualTo(request.dayCode)
    assertThat(startTime).isEqualTo(request.startTime)
    assertThat(endTime).isEqualTo(request.endTime)
    assertThat(effectiveDate).isEqualTo(request.effectiveDate)
    assertThat(expiryDate).isEqualTo(request.expiryDate)
    assertThat(createdBy).isEqualTo(request.createdBy)
    assertThat(createdTime).isCloseTo(request.createdTime, within(2, ChronoUnit.SECONDS))
  }

  fun WebTestClient.createTimeSlot() = this.post()
    .uri("/sync/time-slot")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .bodyValue(createTimeSlotRequest())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<SyncTimeSlot>()
    .returnResult().responseBody!!
}
