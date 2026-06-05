package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotifyCallbackNotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.NotifyCallbackService

@Service
class NotifyCallbackFacade(
  private val notifyCallbackService: NotifyCallbackService,
) {

  fun processCallback(request: NotifyCallbackNotificationRequest, providedSecret: String?) {
    notifyCallbackService.processCallback(request, providedSecret)
  }
}
