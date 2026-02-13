package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.resource.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.location
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.next
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class VisitSlotIntegrationTest : IntegrationTestBase() {

  private val location = moorlandLocation.copy(id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"))

  @BeforeEach
  @Transactional
  fun setupTest() {
    clearAllVisitData()
    prisonerSearchApi().stubGetPrisonName(
      MOORLAND,
      MOORLAND_PRISONER,
    )
    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(
      prisonCode = MOORLAND,
      locations = listOf(location),
    )
  }

  @AfterEach
  @Transactional
  fun tearDown() {
    clearAllVisitData()
  }

  @Test
  fun `should return unauthorized if no token is provided`() {
    webTestClient.post()
      .uri("/admin/time-slot/{prisonTimeSlotId}/visit-slot", 1)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createVisitSlotRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.put()
      .uri("/admin/visit-slot/id/{visitSlotId}", 1)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateVisitSlotRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.delete()
      .uri("/admin/time-slot/{prisonTimeSlotId}", 1)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should create update and delete visit slot for admin role`() {
    val createRequest = createVisitSlotRequest()

    val created = webTestClient.post()
      .uri("/admin/time-slot/{prisonTimeSlotId}/visit-slot", 1)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(
        setAuthorisation(
          username = MOORLAND_PRISON_USER.username,
          roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN"),
        ),
      )
      .bodyValue(createRequest)
      .exchange()
      .expectStatus().isOk
      .expectBody(VisitSlot::class.java)
      .returnResult().responseBody!!

    assertThat(created.prisonTimeSlotId).isEqualTo(1)
    assertThat(created.dpsLocationId).isEqualTo(location.id)

    val updateRequest = updateVisitSlotRequest()

    val updated = webTestClient.put()
      .uri("/admin/visit-slot/id/{visitSlotId}", created.visitSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(
        setAuthorisation(
          username = MOORLAND_PRISON_USER.username,
          roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN"),
        ),
      )
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus().isOk
      .expectBody(VisitSlot::class.java)
      .returnResult().responseBody!!

    assertThat(updated.maxAdults).isEqualTo(8)
    assertThat(updated.maxGroups).isEqualTo(4)
    assertThat(updated.maxVideo).isEqualTo(1)

    webTestClient.delete()
      .uri("/admin/visit-slot/id/{visitSlotId}", created.visitSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(
        setAuthorisation(
          username = MOORLAND_PRISON_USER.username,
          roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN"),
        ),
      )
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  @Test
  fun `should not delete visit slot when official visits exist`() {
    // create slot
    val createRequest = createVisitSlotRequest()

    val created = webTestClient.post()
      .uri("/admin/time-slot/{prisonTimeSlotId}/visit-slot", 1)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(
        setAuthorisation(
          username = MOORLAND_PRISON_USER.username,
          roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN"),
        ),
      )
      .bodyValue(createRequest)
      .exchange()
      .expectStatus().isOk
      .expectBody(VisitSlot::class.java)
      .returnResult().responseBody!!

    personalRelationshipsApi().stubAllApprovedContacts(MOORLAND_PRISONER.number, contactId = 123, prisonerContactId = 456)

    locationsInsidePrisonApi().stubGetOfficialVisitLocationsAtPrison(
      prisonCode = MOORLAND,
      locations = listOf(
        location(prisonCode = MOORLAND, locationKeySuffix = "1-1", localName = "Visit place", id = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247")),
      ),
    )

    prisonerSearchApi().stubFindPrisonersBySearchTerm(MOORLAND, MOORLAND_PRISONER.firstName, MOORLAND_PRISONER)

    // create an official visit using the created slot via TestApiClient
    val officialVisitor = OfficialVisitor(
      visitorTypeCode = VisitorType.CONTACT,
      contactId = 123,
      prisonerContactId = 456,
      relationshipCode = "POM",
      leadVisitor = true,
      assistedVisit = false,
    )

    val officialVisitRequest = CreateOfficialVisitRequest(
      prisonVisitSlotId = created.visitSlotId,
      prisonerNumber = MOORLAND_PRISONER.number,
      visitDate = today().next(DayOfWeek.MONDAY),
      startTime = LocalTime.parse("09:00"),
      endTime = LocalTime.parse("10:00"),
      dpsLocationId = created.dpsLocationId,
      visitTypeCode = VisitType.IN_PERSON,
      staffNotes = "",
      prisonerNotes = "",
      searchTypeCode = SearchLevelType.PAT,
      officialVisitors = listOf(officialVisitor),
    )

    testAPIClient.createOfficialVisit(officialVisitRequest, MOORLAND_PRISON_USER)

    webTestClient.delete()
      .uri("/admin/visit-slot/id/{visitSlotId}", created.visitSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(
        setAuthorisation(
          username = MOORLAND_PRISON_USER.username,
          roles = listOf("ROLE_OFFICIAL_VISIT_ADMIN"),
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(org.springframework.http.HttpStatus.CONFLICT)
      .expectBody().jsonPath("$.userMessage")
      .isEqualTo("The prison visit slot has visits associated with it and cannot be deleted.")
  }

  private fun updateVisitSlotRequest(): UpdateVisitSlotRequest = UpdateVisitSlotRequest(maxAdults = 8, maxGroups = 4, maxVideo = 1)

  private fun createVisitSlotRequest(): CreateVisitSlotRequest = CreateVisitSlotRequest(dpsLocationId = location.id, maxAdults = 10, maxGroups = 5, maxVideo = 2)
}
