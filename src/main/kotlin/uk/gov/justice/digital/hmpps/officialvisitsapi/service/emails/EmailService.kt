package uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails

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
  PLACEHOLDER_EMAIL_TYPE,
}
