package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.extensions.getFullName
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.ContactDetailsDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.PrisonDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEmailStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
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
  private val prisonRegisterClient: PrisonRegisterClient,
  @Value($$"${notify.joining-instructions.video}") val videoJoiningInstructions: String? = null,
  @Value($$"${notify.joining-instructions.in-person}") val inPersonJoiningInstructions: String? = null,
  @Value($$"${notify.joining-instructions.telephone}") val telephoneJoiningInstructions: String? = null,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendNotification(officialVisitId: Long, request: NotificationRequest, user: User): NotificationResponse = run {
    // Get the details of the visit
    val officialVisit = officialVisitRepository.findById(officialVisitId)
      .orElseThrow { EntityNotFoundException("Official visit with id $officialVisitId not found") }

    // Get the location local name/description
    val location = locationsService.getLocationById(officialVisit.dpsLocationId)?.localName ?: "Unknown location"

    // Get the prisoner's basic details
    val prisoner = prisonerSearchClient.getPrisoner(officialVisit.prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner not found ${officialVisit.prisonerNumber}")

    // Get the address and main contact details for the prison
    val prison = prisonRegisterClient.getPrisonDetails(officialVisit.prisonCode)

    // Get the official visits contacts at this prison
    val prisonContact = prisonRegisterClient.getPrisonContactDetails(officialVisit.prisonCode, "OFFICIAL_VISIT")

    val recipients = buildSet {
      request.emailAddresses.map { it.lowercase().trim() }.distinct().forEach { emailAddress ->
        sendOfficialVisitEmail(
          officialVisit.officialVisitId,
          getEmail(
            notificationType = request.notificationType,
            officialVisit = officialVisit,
            emailAddress = emailAddress.trim(),
            videoLinkUrl = request.videoLinkUrl?.trim(),
            notes = request.notes?.trim(),
            prisoner = prisoner,
            location = location,
            prison = prison,
            prisonContact = prisonContact,
          ),
          user,
        )?.let { notificationId -> add(NotificationRecipient(emailAddress, notificationId)) }
      }
    }

    NotificationResponse(officialVisitId, request.notificationType, recipients.toList())
  }

  private fun sendOfficialVisitEmail(officialVisitId: Long, email: Email, user: User): Long? = run {
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
            createdBy = user.username,
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

    val significantEventCount = auditingService.findByOfficialVisitId(officialVisitId)
      .filter { it.eventDateTime > lastNotification.createdTime }
      .filterNot { it.eventVersion == 1 }
      .count { it.significantChange }

    return VisitChangeStatusResponse(hasChanged = (significantEventCount > 0))
  }

  private fun getEmail(
    notificationType: NotificationType,
    officialVisit: OfficialVisitEntity,
    emailAddress: String,
    prisoner: Prisoner,
    location: String,
    prison: PrisonDto? = null,
    prisonContact: ContactDetailsDto? = null,
    videoLinkUrl: String? = null,
    notes: String? = null,
  ): Email = run {
    when (notificationType) {
      NotificationType.CREATE -> {
        when (officialVisit.visitTypeCode) {
          VisitType.IN_PERSON, VisitType.UNKNOWN -> InPersonVisitConfirmedEmail(
            emailAddress = emailAddress,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerName = prisoner.getFullName(),
            prisonDetails = fromPrisonRegisterTypes(prison, prisonContact),
            visitDate = officialVisit.visitDate,
            visitStartTime = officialVisit.startTime,
            visitEndTime = officialVisit.endTime,
            visitLocation = location,
            visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.fullName() },
            notes = notes,
            joiningInstructions = inPersonJoiningInstructions,
          )

          VisitType.VIDEO -> VideoVisitConfirmedEmail(
            emailAddress = emailAddress,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerName = prisoner.getFullName(),
            prisonDetails = fromPrisonRegisterTypes(prison, prisonContact),
            visitDate = officialVisit.visitDate,
            visitStartTime = officialVisit.startTime,
            visitEndTime = officialVisit.endTime,
            visitLocation = location,
            videoLinkUrl = videoLinkUrl,
            visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.fullName() },
            notes = notes,
            joiningInstructions = videoJoiningInstructions,
          )

          VisitType.TELEPHONE -> TelephoneVisitConfirmedEmail(
            emailAddress = emailAddress,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerName = prisoner.getFullName(),
            prisonDetails = fromPrisonRegisterTypes(prison, prisonContact),
            visitDate = officialVisit.visitDate,
            visitStartTime = officialVisit.startTime,
            visitEndTime = officialVisit.endTime,
            visitLocation = location,
            visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.fullName() },
            notes = notes,
            joiningInstructions = telephoneJoiningInstructions,
          )
        }
      }

      NotificationType.AMEND -> {
        when (officialVisit.visitTypeCode) {
          VisitType.IN_PERSON, VisitType.UNKNOWN -> InPersonVisitAmendedEmail(
            emailAddress = emailAddress,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerName = prisoner.getFullName(),
            prisonDetails = fromPrisonRegisterTypes(prison, prisonContact),
            visitDate = officialVisit.visitDate,
            visitStartTime = officialVisit.startTime,
            visitEndTime = officialVisit.endTime,
            visitLocation = location,
            visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.fullName() },
            notes = notes,
            joiningInstructions = inPersonJoiningInstructions,
          )

          VisitType.VIDEO -> VideoVisitAmendedEmail(
            emailAddress = emailAddress,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerName = prisoner.getFullName(),
            prisonDetails = fromPrisonRegisterTypes(prison, prisonContact),
            visitDate = officialVisit.visitDate,
            visitStartTime = officialVisit.startTime,
            visitEndTime = officialVisit.endTime,
            visitLocation = location,
            visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.fullName() },
            videoLinkUrl = videoLinkUrl?.takeIf { officialVisit.visitTypeCode == VisitType.VIDEO },
            notes = notes,
            joiningInstructions = videoJoiningInstructions,
          )

          VisitType.TELEPHONE -> TelephoneVisitAmendedEmail(
            emailAddress = emailAddress,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerName = prisoner.getFullName(),
            prisonDetails = fromPrisonRegisterTypes(prison, prisonContact),
            visitDate = officialVisit.visitDate,
            visitStartTime = officialVisit.startTime,
            visitEndTime = officialVisit.endTime,
            visitLocation = location,
            visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.fullName() },
            notes = notes,
            joiningInstructions = telephoneJoiningInstructions,
          )
        }
      }

      NotificationType.CANCEL -> {
        when (officialVisit.visitTypeCode) {
          VisitType.IN_PERSON, VisitType.UNKNOWN -> InPersonVisitCancelledEmail(
            emailAddress = emailAddress,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerName = prisoner.getFullName(),
            prisonDetails = fromPrisonRegisterTypes(prison, prisonContact),
            visitDate = officialVisit.visitDate,
            visitStartTime = officialVisit.startTime,
            visitEndTime = officialVisit.endTime,
            visitLocation = location,
            visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.fullName() },
            notes = notes,
          )

          VisitType.VIDEO -> VideoVisitCancelledEmail(
            emailAddress = emailAddress,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerName = prisoner.getFullName(),
            prisonDetails = fromPrisonRegisterTypes(prison, prisonContact),
            visitDate = officialVisit.visitDate,
            visitStartTime = officialVisit.startTime,
            visitEndTime = officialVisit.endTime,
            visitLocation = location,
            visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.fullName() },
            notes = notes,
          )

          VisitType.TELEPHONE -> TelephoneVisitCancelledEmail(
            emailAddress = emailAddress,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerName = prisoner.getFullName(),
            prisonDetails = fromPrisonRegisterTypes(prison, prisonContact),
            visitDate = officialVisit.visitDate,
            visitStartTime = officialVisit.startTime,
            visitEndTime = officialVisit.endTime,
            visitLocation = location,
            visitorNames = officialVisit.officialVisitors().joinToString(", ") { it.fullName() },
            notes = notes,
          )
        }
      }
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
    createdBy = createdBy,
    statusUpdatedTime = statusUpdatedTime,
  )

  private fun fromPrisonRegisterTypes(prison: PrisonDto?, prisonContact: ContactDetailsDto?) = PrisonContactDetails(
    prisonCode = prison?.prisonId,
    prisonName = prison?.prisonName,
    prisonAddressLine1 = prison?.addresses?.first()?.addressLine1,
    prisonAddressLine2 = prison?.addresses?.first()?.addressLine2,
    prisonTown = prison?.addresses?.first()?.town,
    prisonCounty = prison?.addresses?.first()?.county,
    prisonPostcode = prison?.addresses?.first()?.postcode,
    prisonEmail = prisonContact?.emailAddress,
    prisonTelephone = prisonContact?.phoneNumber,
    prisonWebsite = prisonContact?.webAddress,
  )
}

data class PrisonContactDetails(
  val prisonCode: String? = null,
  val prisonName: String? = null,
  val prisonAddressLine1: String? = null,
  val prisonAddressLine2: String? = null,
  val prisonTown: String? = null,
  val prisonCounty: String? = null,
  val prisonPostcode: String? = null,
  val prisonEmail: String? = null,
  val prisonTelephone: String? = null,
  val prisonWebsite: String? = null,
)

enum class NotificationType {
  CREATE,
  AMEND,
  CANCEL,
}
