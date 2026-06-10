package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.SentEmailSearchCriteria
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NotificationResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitNotification
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.SentEmailRecord
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.NotificationsService

@Service
class NotificationFacade(
  private val notificationsService: NotificationsService,
) {

  fun sendNotification(officialVisitId: Long, request: NotificationRequest, user: User): NotificationResponse = notificationsService.sendNotification(officialVisitId, request, user)

  fun searchSentEmails(
    prisonCode: String,
    criteria: SentEmailSearchCriteria,
    page: Int,
    size: Int,
    user: User,
  ): PagedModel<SentEmailRecord> = notificationsService.searchSentEmails(prisonCode, criteria, page, size, user)

  fun getNotificationsByOfficialVisitId(officialVisitId: Long, sort: Sort): List<OfficialVisitNotification> = notificationsService.getNotificationsByOfficialVisitId(
    officialVisitId,
    sort,
  )
}
