package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toTitleCase
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

typealias NotificationId = UUID
typealias TemplateId = String

fun interface EmailService {
  fun send(email: Email): Result<Pair<NotificationId, TemplateId>>
}

abstract class Email(val emailAddress: String) {
  private val personalisation: MutableMap<String, String> = mutableMapOf()

  protected fun addPersonalisation(key: String, value: String) {
    personalisation[key] = value
  }

  abstract fun type(): EmailType

  fun personalisation(): Map<String, String> = personalisation.toMap()
}

class EmailTemplates(private val templates: Set<EmailTemplate>) {
  fun templateIdFor(emailType: EmailType): TemplateId? = templates.singleOrNull { it.emailType == emailType }?.templateId
}

data class EmailTemplate(val templateId: TemplateId, val emailType: EmailType)

enum class EmailType {
  OFFICIAL_VISIT_CREATED,
  OFFICIAL_VISIT_UPDATED,
  OFFICIAL_VISIT_CANCELLED,
}

class OfficialVisitCreatedEmail(
  emailAddress: String,
  prisonerName: String,
  appointmentDate: LocalDate,
  appointmentTime: LocalTime,
  appointmentLocation: String,
  videoLinkUrl: String? = null,
  notes: String? = null,
  userName: String,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("appointment_date", appointmentDate.toMediumFormatStyle())
    addPersonalisation("appointment_time", appointmentTime.toHourMinuteStyle())
    addPersonalisation("appointment_location", appointmentLocation)
    addPersonalisation("show_video_link", "yes".takeIf { videoLinkUrl?.isNotBlank() == true } ?: "no")
    addPersonalisation("video_link_url", videoLinkUrl?.takeIf { it.isNotBlank() } ?: "")
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
    addPersonalisation("user_name", userName)
  }

  override fun type(): EmailType = EmailType.OFFICIAL_VISIT_CREATED
}

class OfficialVisitUpdatedEmail(
  emailAddress: String,
  prisonerName: String,
  appointmentDate: LocalDate,
  appointmentTime: LocalTime,
  appointmentLocation: String,
  videoLinkUrl: String? = null,
  notes: String? = null,
  userName: String,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("appointment_date", appointmentDate.toMediumFormatStyle())
    addPersonalisation("appointment_time", appointmentTime.toHourMinuteStyle())
    addPersonalisation("appointment_location", appointmentLocation)
    addPersonalisation("show_video_link", "yes".takeIf { videoLinkUrl?.isNotBlank() == true } ?: "no")
    addPersonalisation("video_link_url", videoLinkUrl?.takeIf { it.isNotBlank() } ?: "")
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
    addPersonalisation("user_name", userName)
  }

  override fun type(): EmailType = EmailType.OFFICIAL_VISIT_UPDATED
}

class OfficialVisitCancelledEmail(
  emailAddress: String,
  prisonerName: String,
  visitorNames: String,
  appointmentDate: LocalDate,
  appointmentTime: LocalTime,
  appointmentLocation: String,
  notes: String? = null,
  userName: String,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("appointment_date", appointmentDate.toMediumFormatStyle())
    addPersonalisation("appointment_time", appointmentTime.toHourMinuteStyle())
    addPersonalisation("appointment_location", appointmentLocation)
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
    addPersonalisation("user_name", userName)
  }

  override fun type(): EmailType = EmailType.OFFICIAL_VISIT_CANCELLED
}
