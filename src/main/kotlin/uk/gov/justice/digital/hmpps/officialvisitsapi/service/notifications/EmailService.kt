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
  IN_PERSON_VISIT_CONFIRMED,
  IN_PERSON_VISIT_AMENDED,
  IN_PERSON_VISIT_CANCELLED,
  TELEPHONE_VISIT_CONFIRMED,
  TELEPHONE_VISIT_AMENDED,
  TELEPHONE_VISIT_CANCELLED,
  VIDEO_VISIT_CONFIRMED,
  VIDEO_VISIT_AMENDED,
  VIDEO_VISIT_CANCELLED,
}

class InPersonVisitConfirmedEmail(
  emailAddress: String,
  prisonerNumber: String,
  prisonerName: String,
  prisonDetails: PrisonContactDetails,
  visitDate: LocalDate,
  visitStartTime: LocalTime,
  visitEndTime: LocalTime,
  visitLocation: String,
  visitorNames: String,
  notes: String? = null,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_number", prisonerNumber)
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("prison_code", prisonDetails.prisonCode ?: "")
    addPersonalisation("prison_name", prisonDetails.prisonName ?: "")
    addPersonalisation("prison_address_line1", prisonDetails.prisonAddressLine1 ?: "")
    addPersonalisation("prison_address_line2", prisonDetails.prisonAddressLine2 ?: "")
    addPersonalisation("prison_town", prisonDetails.prisonTown ?: "")
    addPersonalisation("prison_county", prisonDetails.prisonCounty ?: "")
    addPersonalisation("prison_postcode", prisonDetails.prisonPostcode ?: "")
    addPersonalisation("prison_email", prisonDetails.prisonEmail ?: "")
    addPersonalisation("prison_telephone", prisonDetails.prisonTelephone ?: "")
    addPersonalisation("prison_website", prisonDetails.prisonWebsite ?: "")
    addPersonalisation("visit_date", visitDate.toMediumFormatStyle())
    addPersonalisation("visit_start_time", visitStartTime.toHourMinuteStyle())
    addPersonalisation("visit_end_time", visitEndTime.toHourMinuteStyle())
    addPersonalisation("visit_location", visitLocation)
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
  }

  override fun type(): EmailType = EmailType.IN_PERSON_VISIT_CONFIRMED
}

class InPersonVisitAmendedEmail(
  emailAddress: String,
  prisonerNumber: String,
  prisonerName: String,
  prisonDetails: PrisonContactDetails,
  visitDate: LocalDate,
  visitStartTime: LocalTime,
  visitEndTime: LocalTime,
  visitLocation: String,
  visitorNames: String,
  notes: String? = null,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_number", prisonerNumber)
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("prison_code", prisonDetails.prisonCode ?: "")
    addPersonalisation("prison_name", prisonDetails.prisonName ?: "")
    addPersonalisation("prison_address_line1", prisonDetails.prisonAddressLine1 ?: "")
    addPersonalisation("prison_address_line2", prisonDetails.prisonAddressLine2 ?: "")
    addPersonalisation("prison_town", prisonDetails.prisonTown ?: "")
    addPersonalisation("prison_county", prisonDetails.prisonCounty ?: "")
    addPersonalisation("prison_postcode", prisonDetails.prisonPostcode ?: "")
    addPersonalisation("prison_email", prisonDetails.prisonEmail ?: "")
    addPersonalisation("prison_telephone", prisonDetails.prisonTelephone ?: "")
    addPersonalisation("prison_website", prisonDetails.prisonWebsite ?: "")
    addPersonalisation("visit_date", visitDate.toMediumFormatStyle())
    addPersonalisation("visit_start_time", visitStartTime.toHourMinuteStyle())
    addPersonalisation("visit_end_time", visitEndTime.toHourMinuteStyle())
    addPersonalisation("visit_location", visitLocation)
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
  }
  override fun type(): EmailType = EmailType.IN_PERSON_VISIT_AMENDED
}

class InPersonVisitCancelledEmail(
  emailAddress: String,
  prisonerNumber: String,
  prisonerName: String,
  prisonDetails: PrisonContactDetails,
  visitDate: LocalDate,
  visitStartTime: LocalTime,
  visitEndTime: LocalTime,
  visitLocation: String,
  visitorNames: String,
  notes: String? = null,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_number", prisonerNumber)
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("prison_code", prisonDetails.prisonCode ?: "")
    addPersonalisation("prison_name", prisonDetails.prisonName ?: "")
    addPersonalisation("prison_address_line1", prisonDetails.prisonAddressLine1 ?: "")
    addPersonalisation("prison_address_line2", prisonDetails.prisonAddressLine2 ?: "")
    addPersonalisation("prison_town", prisonDetails.prisonTown ?: "")
    addPersonalisation("prison_county", prisonDetails.prisonCounty ?: "")
    addPersonalisation("prison_postcode", prisonDetails.prisonPostcode ?: "")
    addPersonalisation("prison_email", prisonDetails.prisonEmail ?: "")
    addPersonalisation("prison_telephone", prisonDetails.prisonTelephone ?: "")
    addPersonalisation("prison_website", prisonDetails.prisonWebsite ?: "")
    addPersonalisation("visit_date", visitDate.toMediumFormatStyle())
    addPersonalisation("visit_start_time", visitStartTime.toHourMinuteStyle())
    addPersonalisation("visit_end_time", visitEndTime.toHourMinuteStyle())
    addPersonalisation("visit_location", visitLocation)
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
  }

  override fun type(): EmailType = EmailType.IN_PERSON_VISIT_CANCELLED
}

class VideoVisitConfirmedEmail(
  emailAddress: String,
  prisonerNumber: String,
  prisonerName: String,
  prisonDetails: PrisonContactDetails,
  visitDate: LocalDate,
  visitStartTime: LocalTime,
  visitEndTime: LocalTime,
  visitLocation: String,
  visitorNames: String,
  notes: String? = null,
  videoLinkUrl: String? = null,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_number", prisonerNumber)
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("prison_code", prisonDetails.prisonCode ?: "")
    addPersonalisation("prison_name", prisonDetails.prisonName ?: "")
    addPersonalisation("prison_address_line1", prisonDetails.prisonAddressLine1 ?: "")
    addPersonalisation("prison_address_line2", prisonDetails.prisonAddressLine2 ?: "")
    addPersonalisation("prison_town", prisonDetails.prisonTown ?: "")
    addPersonalisation("prison_county", prisonDetails.prisonCounty ?: "")
    addPersonalisation("prison_postcode", prisonDetails.prisonPostcode ?: "")
    addPersonalisation("prison_email", prisonDetails.prisonEmail ?: "")
    addPersonalisation("prison_telephone", prisonDetails.prisonTelephone ?: "")
    addPersonalisation("prison_website", prisonDetails.prisonWebsite ?: "")
    addPersonalisation("visit_date", visitDate.toMediumFormatStyle())
    addPersonalisation("visit_start_time", visitStartTime.toHourMinuteStyle())
    addPersonalisation("visit_end_time", visitEndTime.toHourMinuteStyle())
    addPersonalisation("visit_location", visitLocation)
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
    addPersonalisation("show_video_link", "yes".takeIf { videoLinkUrl?.isNotBlank() == true } ?: "no")
    addPersonalisation("video_link_url", videoLinkUrl?.takeIf { it.isNotBlank() } ?: "")
  }

  override fun type(): EmailType = EmailType.VIDEO_VISIT_CONFIRMED
}

class VideoVisitAmendedEmail(
  emailAddress: String,
  prisonerNumber: String,
  prisonerName: String,
  prisonDetails: PrisonContactDetails,
  visitDate: LocalDate,
  visitStartTime: LocalTime,
  visitEndTime: LocalTime,
  visitLocation: String,
  visitorNames: String,
  notes: String? = null,
  videoLinkUrl: String? = null,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_number", prisonerNumber)
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("prison_code", prisonDetails.prisonCode ?: "")
    addPersonalisation("prison_name", prisonDetails.prisonName ?: "")
    addPersonalisation("prison_address_line1", prisonDetails.prisonAddressLine1 ?: "")
    addPersonalisation("prison_address_line2", prisonDetails.prisonAddressLine2 ?: "")
    addPersonalisation("prison_town", prisonDetails.prisonTown ?: "")
    addPersonalisation("prison_county", prisonDetails.prisonCounty ?: "")
    addPersonalisation("prison_postcode", prisonDetails.prisonPostcode ?: "")
    addPersonalisation("prison_email", prisonDetails.prisonEmail ?: "")
    addPersonalisation("prison_telephone", prisonDetails.prisonTelephone ?: "")
    addPersonalisation("prison_website", prisonDetails.prisonWebsite ?: "")
    addPersonalisation("visit_date", visitDate.toMediumFormatStyle())
    addPersonalisation("visit_start_time", visitStartTime.toHourMinuteStyle())
    addPersonalisation("visit_end_time", visitEndTime.toHourMinuteStyle())
    addPersonalisation("visit_location", visitLocation)
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
    addPersonalisation("show_video_link", "yes".takeIf { videoLinkUrl?.isNotBlank() == true } ?: "no")
    addPersonalisation("video_link_url", videoLinkUrl?.takeIf { it.isNotBlank() } ?: "")
  }

  override fun type(): EmailType = EmailType.VIDEO_VISIT_AMENDED
}

class VideoVisitCancelledEmail(
  emailAddress: String,
  prisonerNumber: String,
  prisonerName: String,
  prisonDetails: PrisonContactDetails,
  visitDate: LocalDate,
  visitStartTime: LocalTime,
  visitEndTime: LocalTime,
  visitLocation: String,
  visitorNames: String,
  notes: String? = null,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_number", prisonerNumber)
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("prison_code", prisonDetails.prisonCode ?: "")
    addPersonalisation("prison_name", prisonDetails.prisonName ?: "")
    addPersonalisation("prison_address_line1", prisonDetails.prisonAddressLine1 ?: "")
    addPersonalisation("prison_address_line2", prisonDetails.prisonAddressLine2 ?: "")
    addPersonalisation("prison_town", prisonDetails.prisonTown ?: "")
    addPersonalisation("prison_county", prisonDetails.prisonCounty ?: "")
    addPersonalisation("prison_postcode", prisonDetails.prisonPostcode ?: "")
    addPersonalisation("prison_email", prisonDetails.prisonEmail ?: "")
    addPersonalisation("prison_telephone", prisonDetails.prisonTelephone ?: "")
    addPersonalisation("prison_website", prisonDetails.prisonWebsite ?: "")
    addPersonalisation("visit_date", visitDate.toMediumFormatStyle())
    addPersonalisation("visit_start_time", visitStartTime.toHourMinuteStyle())
    addPersonalisation("visit_end_time", visitEndTime.toHourMinuteStyle())
    addPersonalisation("visit_location", visitLocation)
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
  }

  override fun type(): EmailType = EmailType.VIDEO_VISIT_CANCELLED
}

class TelephoneVisitConfirmedEmail(
  emailAddress: String,
  prisonerNumber: String,
  prisonerName: String,
  prisonDetails: PrisonContactDetails,
  visitDate: LocalDate,
  visitStartTime: LocalTime,
  visitEndTime: LocalTime,
  visitLocation: String,
  visitorNames: String,
  notes: String? = null,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_number", prisonerNumber)
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("prison_code", prisonDetails.prisonCode ?: "")
    addPersonalisation("prison_name", prisonDetails.prisonName ?: "")
    addPersonalisation("prison_address_line1", prisonDetails.prisonAddressLine1 ?: "")
    addPersonalisation("prison_address_line2", prisonDetails.prisonAddressLine2 ?: "")
    addPersonalisation("prison_town", prisonDetails.prisonTown ?: "")
    addPersonalisation("prison_county", prisonDetails.prisonCounty ?: "")
    addPersonalisation("prison_postcode", prisonDetails.prisonPostcode ?: "")
    addPersonalisation("prison_email", prisonDetails.prisonEmail ?: "")
    addPersonalisation("prison_telephone", prisonDetails.prisonTelephone ?: "")
    addPersonalisation("prison_website", prisonDetails.prisonWebsite ?: "")
    addPersonalisation("visit_date", visitDate.toMediumFormatStyle())
    addPersonalisation("visit_start_time", visitStartTime.toHourMinuteStyle())
    addPersonalisation("visit_end_time", visitEndTime.toHourMinuteStyle())
    addPersonalisation("visit_location", visitLocation)
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
  }

  override fun type(): EmailType = EmailType.TELEPHONE_VISIT_CONFIRMED
}

class TelephoneVisitAmendedEmail(
  emailAddress: String,
  prisonerNumber: String,
  prisonerName: String,
  prisonDetails: PrisonContactDetails,
  visitDate: LocalDate,
  visitStartTime: LocalTime,
  visitEndTime: LocalTime,
  visitLocation: String,
  visitorNames: String,
  notes: String? = null,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_number", prisonerNumber)
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("prison_code", prisonDetails.prisonCode ?: "")
    addPersonalisation("prison_name", prisonDetails.prisonName ?: "")
    addPersonalisation("prison_address_line1", prisonDetails.prisonAddressLine1 ?: "")
    addPersonalisation("prison_address_line2", prisonDetails.prisonAddressLine2 ?: "")
    addPersonalisation("prison_town", prisonDetails.prisonTown ?: "")
    addPersonalisation("prison_county", prisonDetails.prisonCounty ?: "")
    addPersonalisation("prison_postcode", prisonDetails.prisonPostcode ?: "")
    addPersonalisation("prison_email", prisonDetails.prisonEmail ?: "")
    addPersonalisation("prison_telephone", prisonDetails.prisonTelephone ?: "")
    addPersonalisation("prison_website", prisonDetails.prisonWebsite ?: "")
    addPersonalisation("visit_date", visitDate.toMediumFormatStyle())
    addPersonalisation("visit_start_time", visitStartTime.toHourMinuteStyle())
    addPersonalisation("visit_end_time", visitEndTime.toHourMinuteStyle())
    addPersonalisation("visit_location", visitLocation)
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
  }

  override fun type(): EmailType = EmailType.TELEPHONE_VISIT_AMENDED
}

class TelephoneVisitCancelledEmail(
  emailAddress: String,
  prisonerNumber: String,
  prisonerName: String,
  prisonDetails: PrisonContactDetails,
  visitDate: LocalDate,
  visitStartTime: LocalTime,
  visitEndTime: LocalTime,
  visitLocation: String,
  visitorNames: String,
  notes: String? = null,
) : Email(emailAddress) {
  init {
    addPersonalisation("prisoner_number", prisonerNumber)
    addPersonalisation("prisoner_name", prisonerName.toTitleCase())
    addPersonalisation("prison_code", prisonDetails.prisonCode ?: "")
    addPersonalisation("prison_name", prisonDetails.prisonName ?: "")
    addPersonalisation("prison_address_line1", prisonDetails.prisonAddressLine1 ?: "")
    addPersonalisation("prison_address_line2", prisonDetails.prisonAddressLine2 ?: "")
    addPersonalisation("prison_town", prisonDetails.prisonTown ?: "")
    addPersonalisation("prison_county", prisonDetails.prisonCounty ?: "")
    addPersonalisation("prison_postcode", prisonDetails.prisonPostcode ?: "")
    addPersonalisation("prison_email", prisonDetails.prisonEmail ?: "")
    addPersonalisation("prison_telephone", prisonDetails.prisonTelephone ?: "")
    addPersonalisation("prison_website", prisonDetails.prisonWebsite ?: "")
    addPersonalisation("visit_date", visitDate.toMediumFormatStyle())
    addPersonalisation("visit_start_time", visitStartTime.toHourMinuteStyle())
    addPersonalisation("visit_end_time", visitEndTime.toHourMinuteStyle())
    addPersonalisation("visit_location", visitLocation)
    addPersonalisation("visitor_names", visitorNames.toTitleCase())
    addPersonalisation("show_notes", "yes".takeIf { notes?.isNotBlank() == true } ?: "no")
    addPersonalisation("notes", notes?.takeIf { it.isNotBlank() } ?: "")
  }

  override fun type(): EmailType = EmailType.TELEPHONE_VISIT_CANCELLED
}
