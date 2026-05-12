package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NotificationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NotificationRecipient
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NotificationResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.Email
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.OfficialVisitCreatedEmail
import java.time.LocalDateTime

@Component
class NotificationsFacade(
  private val officialVisitRepository: OfficialVisitRepository,
  private val locationsService: LocationsService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun sendNotification(officialVisitId: Long, request: NotificationRequest, user: User): NotificationResponse = run {
    val officialVisit = officialVisitRepository.findById(officialVisitId)
      .orElseThrow { EntityNotFoundException("Official visit with id $officialVisitId not found") }

    val location = locationsService.getLocationById(officialVisit.dpsLocationId)?.localName ?: "Unknown location"
    val prisoner = prisonerSearchClient.getPrisoner(officialVisit.prisonerNumber) ?: throw EntityNotFoundException("Prisoner not found ${officialVisit.prisonerNumber}")

    val recipients = buildSet {
      request.emailAddresses.distinct().forEach { emailAddress ->
        sendOfficialVisitEmail(
          officialVisit.officialVisitId,
          getEmail(request.notificationType!!, officialVisit, emailAddress, prisoner, location, user),
        )?.let { notificationId -> add(NotificationRecipient(emailAddress, notificationId)) }
      }
    }

    NotificationResponse(officialVisitId, request.notificationType!!, recipients.toList())
  }

  /**
   * Will return the identifier of the notification created if successful, otherwise null.
   */
  @Transactional
  fun sendOfficialVisitEmail(officialVisitId: Long, email: Email): Long? = run {
    var notificationId: Long? = null

    emailService.send(email)
      .onSuccess { (govNotifyNotificationId, templateId) ->
        notificationRepository.saveAndFlush(
          NotificationEntity(
            officialVisitId = officialVisitId,
            templateId = templateId,
            emailAddress = email.emailAddress,
            reason = email.type().name,
            govNotifyNotificationId = govNotifyNotificationId,
            createdTime = LocalDateTime.now(),
          ).also { notificationId = it.notificationId },
        )
      }
      .onFailure { exception -> logger.info("Failed to send email ${email.type()}.", exception) }

    notificationId
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

      else -> throw IllegalArgumentException("Unknown notification type $notificationType")
    }
  }
}

enum class NotificationType {
  CREATE,
  AMEND,
  CANCEL,
}
