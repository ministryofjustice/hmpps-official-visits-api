package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncVisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.VisitSlotInfo
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class SyncVisitSlotIntegrationTest : IntegrationTestBase() {
  private var savedPrisonVisitSlotId = 0L

  private val createdTime = LocalDateTime.now().minusDays(2)
  private val updatedTime = LocalDateTime.now().minusDays(1)

  @BeforeEach
  fun initialiseData() {
    savedPrisonVisitSlotId = (webTestClient.createVisitSlot()).visitSlotId
    stubEvents.reset()
  }

  @Test
  fun `should create a new prison visit slot`() {
    val syncVisitSlot = webTestClient.createVisitSlot()
    syncVisitSlot.assertWithCreateRequest(createVisitSlotRequest())
    assertThat(syncVisitSlot.visitSlotId).isGreaterThan(0)
    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_SLOT_CREATED,
      additionalInfo = VisitSlotInfo(
        visitSlotId = syncVisitSlot.visitSlotId,
        source = Source.NOMIS,
        username = "OFFICIAL_VISITS_SERVICE",
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should update an existing visit slot`() {
    val updateRequest = updateVisitSlotRequest()

    val syncVisitSlot = webTestClient.put()
      .uri("/sync/visit-slot/{prisonVisitSlotId}", savedPrisonVisitSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncVisitSlot>()
      .returnResult().responseBody!!

    assertThat(syncVisitSlot.visitSlotId).isEqualTo(savedPrisonVisitSlotId)
    assertThat(syncVisitSlot.maxAdults).isEqualTo(15)

    stubEvents.assertHasEvent(
      event = OutboundEvent.VISIT_SLOT_UPDATED,
      additionalInfo = VisitSlotInfo(
        visitSlotId = syncVisitSlot.visitSlotId,
        source = Source.NOMIS,
        username = "OFFICIAL_VISITS_SERVICE",
        prisonId = MOORLAND,
      ),
    )
  }

  @Test
  fun `should get an existing visit slot by ID`() {
    val syncVisitSlot = webTestClient.get()
      .uri("/sync/visit-slot/{prisonVisitSlotId}", savedPrisonVisitSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<SyncVisitSlot>()
      .returnResult().responseBody!!

    syncVisitSlot.assertWithCreateRequest(createVisitSlotRequest())
    assertThat(syncVisitSlot.visitSlotId).isGreaterThan(0)
  }

  private fun SyncVisitSlot.assertWithCreateRequest(request: SyncCreateVisitSlotRequest) {
    assertThat(maxAdults).isEqualTo(request.maxAdults)
    assertThat(dpsLocationId).isEqualTo(request.dpsLocationId)
    assertThat(createdBy).isEqualTo(request.createdBy)
    assertThat(createdTime).isCloseTo(request.createdTime, within(2, ChronoUnit.SECONDS))
  }

  private fun createVisitSlotRequest() = SyncCreateVisitSlotRequest(
    prisonTimeSlotId = 1L,
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    maxAdults = 10,
    createdBy = "Test",
    createdTime = createdTime,
  )

  private fun updateVisitSlotRequest() = SyncUpdateVisitSlotRequest(
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    updatedBy = "Test",
    maxAdults = 15,
    updatedTime = updatedTime,
    prisonTimeSlotId = 1L,
  )

  fun WebTestClient.createVisitSlot() = this.post()
    .uri("/sync/visit-slot")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(username = MOORLAND_PRISON_USER.username, roles = listOf("OFFICIAL_VISITS_MIGRATION")))
    .bodyValue(createVisitSlotRequest())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<SyncVisitSlot>()
    .returnResult().responseBody!!
}
