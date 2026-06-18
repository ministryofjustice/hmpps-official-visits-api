package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationSearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NotificationRecipient
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NotificationResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitNotification
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.SentNotification
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.VisitChangeStatusResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import java.time.LocalDateTime

@Component
@Transactional
class NotificationsService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val locationsService: LocationsService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
  private val sentNotificationsService: SentNotificationsService,
  private val auditingService: AuditingService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendNotification(officialVisitId: Long, request: NotificationRequest, user: User): NotificationResponse = run {
    val officialVisit = officialVisitRepository.findById(officialVisitId)
      .orElseThrow { EntityNotFoundException("Official visit with id $officialVisitId not found") }

    val location = locationsService.getLocationById(officialVisit.dpsLocationId)?.localName ?: "Unknown location"
    val prisoner = prisonerSearchClient.getPrisoner(officialVisit.prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner not found ${officialVisit.prisonerNumber}")

    val recipients = buildSet {
      request.emailAddresses.distinct().forEach { emailAddress ->
        sendOfficialVisitEmail(
          officialVisit.officialVisitId,
          getEmail(request.notificationType, officialVisit, emailAddress, prisoner, location, user),
        )?.let { notificationId -> add(NotificationRecipient(emailAddress, notificationId)) }
      }
    }

    NotificationResponse(officialVisitId, request.notificationType, recipients.toList())
  }

  fun sendOfficialVisitEmail(officialVisitId: Long, email: Email): Long? = run {
    var notificationId: Long? = null
    logger.info("sending email ${email.type()} officialVisitId $officialVisitId")
    emailService.send(email)
      .onSuccess { (govNotifyNotificationId, templateId) ->
        notificationRepository.saveAndFlush(
          NotificationEntity(
            officialVisitId = officialVisitId,
            templateId = templateId,
            emailAddress = email.emailAddress,
            reason = email.type().name,
            govNotifyNotificationId = govNotifyNotificationId,
            emailStatus = NotificationEmailStatus.PENDING,
            createdTime = LocalDateTime.now(),
          ),
        ).also {
          notificationId = it.notificationId
          logger.info("sent notification with notification id $notificationId.")
        }
      }
      .onFailure { exception -> logger.info("Failed to send email ${email.type()}.", exception) }

    notificationId
  }

  @Transactional(readOnly = true)
  fun searchSentNotifications(
    prisonCode: String,
    request: NotificationSearchRequest,
    page: Int,
    size: Int,
    user: User,
  ): PagedModel<SentNotification> = sentNotificationsService.searchSentNotifications(prisonCode, request, page, size, user)

  @Transactional(readOnly = true)
  fun getNotificationsByOfficialVisitId(officialVisitId: Long, sort: Sort): List<OfficialVisitNotification> = run {
    officialVisitRepository.findById(officialVisitId)
      .orElseThrow { EntityNotFoundException("Official visit with id $officialVisitId not found") }
    notificationRepository.findByOfficialVisitId(officialVisitId, sort)
      .map { it.toOfficialVisitNotification() }
  }

  @Transactional(readOnly = true)
  fun checkVisitChangedSinceLastNotification(officialVisitId: Long): VisitChangeStatusResponse {
    val lastNotification = notificationRepository.findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId)
      ?: return VisitChangeStatusResponse(hasChanged = false)

    val inScopeEvents = auditingService.findByOfficialVisitId(officialVisitId)
      .filter { it.eventDateTime > lastNotification.createdTime }
      .filterNot { it.eventVersion == 1 }

    val cancelledEvents = inScopeEvents
      .filter { it.eventType == "CANCELLED" }
      .size

    val significantEvents = inScopeEvents
      .filter { it.eventChanges.any { change -> change.significantChange } }
      .size

    return VisitChangeStatusResponse(hasChanged = (cancelledEvents > 0 || significantEvents > 0))
  }

  private fun getEmail(
    notificationType: NotificationType,
    officialVisit: OfficialVisitEntity,
    emailAddress: String,
    prisoner: Prisoner,
    location: String,
    user: User,
  ): Email = run {
    when (notificationType) {
      NotificationType.CREATE -> OfficialVisitCreatedEmail(
        emailAddress = emailAddress,
        prisonerName = prisoner.firstName + " " + prisoner.lastName,
        appointmentDate = officialVisit.visitDate,
        appointmentTime = officialVisit.startTime,
        appointmentLocation = location,
        userName = user.name,
      )

      NotificationType.AMEND -> OfficialVisitUpdatedEmail(
        emailAddress = emailAddress,
        prisonerName = prisoner.firstName + " " + prisoner.lastName,
        appointmentDate = officialVisit.visitDate,
        appointmentTime = officialVisit.startTime,
        appointmentLocation = location,
        userName = user.name,
      )

      NotificationType.CANCEL -> OfficialVisitCancelledEmail(
        emailAddress = emailAddress,
        prisonerName = prisoner.firstName + " " + prisoner.lastName,
        visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.firstName + " " + it.lastName },
        appointmentDate = officialVisit.visitDate,
        appointmentTime = officialVisit.startTime,
        appointmentLocation = location,
        userName = user.name,
      )
    }
  }

  private fun NotificationEntity.toOfficialVisitNotification() = OfficialVisitNotification(
    notificationId = notificationId,
    officialVisitId = officialVisitId,
    templateId = templateId,
    emailAddress = emailAddress,
    reason = reason,
    govNotifyNotificationId = govNotifyNotificationId,
    emailStatus = emailStatus,
    createdTime = createdTime,
    statusUpdatedTime = statusUpdatedTime,
  )
}

enum class NotificationType {
  CREATE,
  AMEND,
  CANCEL,
}
