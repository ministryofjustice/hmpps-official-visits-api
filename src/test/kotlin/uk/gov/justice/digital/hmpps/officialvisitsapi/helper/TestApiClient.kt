package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import org.springframework.data.web.PagedModel
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCompletionRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateTimeSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AuditedEventResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NotificationResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitsForReviewCountResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitsForReviewResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.NotificationType
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class TestApiClient(private val webTestClient: WebTestClient, private val jwtAuthHelper: JwtAuthorisationHelper) {
  fun createOfficialVisit(request: CreateOfficialVisitRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = webTestClient
    .post()
    .uri("/official-visit/prison/${prisonUser.caseloads.first()}")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(CreateOfficialVisitResponse::class.java)
    .returnResult().responseBody!!

  fun getOfficialVisitBy(officialVisitId: Long, prisonUser: PrisonUser) = webTestClient
    .get()
    .uri("/official-visit/prison/${prisonUser.caseloads.first()}/id/$officialVisitId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(OfficialVisitDetails::class.java)
    .returnResult().responseBody!!

  fun cancel(officialVisitId: Long, request: OfficialVisitCancellationRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = webTestClient
    .post()
    .uri("/official-visit/prison/${prisonUser.caseloads.first()}/id/$officialVisitId/cancel")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk

  fun complete(officialVisitId: Long, request: OfficialVisitCompletionRequest, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = webTestClient
    .post()
    .uri("/official-visit/prison/${prisonUser.caseloads.first()}/id/$officialVisitId/complete")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk

  fun sendNotification(
    officialVisitId: Long,
    notificationType: NotificationType,
    emailAddresses: List<String> = listOf("test@example.com"),
    prisonUser: PrisonUser = MOORLAND_PRISON_USER,
  ) = webTestClient
    .post()
    .uri("/notification/$officialVisitId")
    .bodyValue(NotificationRequest(notificationType = notificationType, emailAddresses = emailAddresses))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<NotificationResponse>()
    .returnResult().responseBody!!

  fun getAuditedEventsByOfficialVisitID(officialVisitId: Long, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = webTestClient
    .get()
    .uri("/official-visit/id/$officialVisitId/audited-events")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS__R")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<List<AuditedEventResponse>>()
    .returnResult().responseBody!!

  fun getVisitsForReviewCount(prisonUser: PrisonUser = MOORLAND_PRISON_USER) = webTestClient
    .get()
    .uri("/visit-review/prison/${prisonUser.caseloads.first()}/count")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<VisitsForReviewCountResponse>()
    .returnResult().responseBody!!

  fun getVisitsForReviewList(prisonUser: PrisonUser = MOORLAND_PRISON_USER) = webTestClient
    .get()
    .uri("/visit-review/prison/${prisonUser.caseloads.first()}/list?page=0&size=10")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<VisitsForReviewResponseResponse>()
    .returnResult().responseBody!!

  fun runJob(jobName: String) = webTestClient
    .post()
    .uri("/job-admin/run/$jobName")
    .accept(MediaType.TEXT_PLAIN)
    .exchange()
    .expectStatus().isOk

  fun acknowledgeVisitForReview(visitReviewId: Long, prisonUser: PrisonUser = MOORLAND_PRISON_USER) = webTestClient
    .put()
    .uri("/visit-review/prison/${prisonUser.caseloads.first()}/id/$visitReviewId/acknowledge")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(prisonUser, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
    .exchange()
    .expectStatus().isOk

  fun generateVisitSlot(futureVisitDate: LocalDate): Pair<TimeSlot, uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot> {
    val timeSlot = webTestClient.post()
      .uri("/admin/time-slot")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(MOORLAND_PRISON_USER, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
      .bodyValue(createTimeSlotRequest(futureVisitDate))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<TimeSlot>()
      .returnResult().responseBody!!
    val createRequest = createVisitSlotRequest(moorlandLocation.id)

    val visitSlot = webTestClient.post()
      .uri("/admin/time-slot/{prisonTimeSlotId}/visit-slot", timeSlot.prisonTimeSlotId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(MOORLAND_PRISON_USER, roles = listOf("ROLE_OFFICIAL_VISITS_ADMIN")))
      .bodyValue(createRequest)
      .exchange()
      .expectStatus().isOk
      .expectBody<uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot>()
      .returnResult().responseBody!!
    return Pair(timeSlot, visitSlot)
  }

  private fun setAuthorisation(prisonUser: PrisonUser, roles: List<String>): (HttpHeaders) -> Unit = run {
    jwtAuthHelper.setAuthorisationHeader(
      username = prisonUser.username,
      scope = listOf("read"),
      roles = roles,
    )
  }

  private fun createVisitSlotRequest(dpsLocationId: UUID = moorlandLocation.id): CreateVisitSlotRequest = CreateVisitSlotRequest(dpsLocationId = dpsLocationId, maxAdults = 10, maxGroups = 5, maxVideo = 2)

  private fun createTimeSlotRequest(visitDate: LocalDate) = CreateTimeSlotRequest(
    prisonCode = MOORLAND,
    dayCode = getDayCode(visitDate),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    effectiveDate = LocalDate.now().plusDays(1),
    expiryDate = LocalDate.now().plusDays(365),
  )

  private fun getDayCode(date: LocalDate): DayType = when (date.dayOfWeek) {
    DayOfWeek.MONDAY -> DayType.MON
    DayOfWeek.TUESDAY -> DayType.TUE
    DayOfWeek.WEDNESDAY -> DayType.WED
    DayOfWeek.THURSDAY -> DayType.THU
    DayOfWeek.FRIDAY -> DayType.FRI
    DayOfWeek.SATURDAY -> DayType.SAT
    DayOfWeek.SUNDAY -> DayType.SUN
  }

  data class VisitsForReviewResponseResponse(
    val content: List<VisitsForReviewResponse>,
    val page: PagedModel.PageMetadata,
  )
}
